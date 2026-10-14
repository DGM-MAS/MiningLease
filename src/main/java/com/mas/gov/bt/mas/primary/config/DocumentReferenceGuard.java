package com.mas.gov.bt.mas.primary.config;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Rejects request bodies that point a workflow field (…DocId / …FileId) at a document that does
 * not exist, is not ACTIVE, or — for citizen callers — that they cannot themselves open.
 * Previously any value, including a victim's permit or a non-existent id, was accepted and
 * persisted on the application (PT report 2026-10-09, finding 5.7). A citizen "can open" a
 * document when they uploaded it or it is attached to an application they own, the same rule the
 * file-upload service applies on read.
 */
@ControllerAdvice
public class DocumentReferenceGuard extends RequestBodyAdviceAdapter {

    private static final Pattern DOC_FIELD = Pattern.compile("(?i)^.*(doc|file)ids?$");
    private static final int MAX_DEPTH = 3;

    private final JdbcTemplate jdbc;

    public DocumentReferenceGuard(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (body == null) {
            return body;
        }
        List<Long> ids = new ArrayList<>();
        collect(body, ids, 0);
        if (ids.isEmpty()) {
            return body;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            return body; // permitAll callbacks carry no user; nothing to authorise against
        }
        Object uid = jwt.getClaim("userId");
        Long userId = uid instanceof Number n ? n.longValue() : null;
        boolean citizen = !"Agency".equalsIgnoreCase(jwt.getClaimAsString("userType"))
                && !"USER".equalsIgnoreCase(jwt.getClaimAsString("userType"));
        for (Long id : ids) {
            check(id, userId, citizen);
        }
        return body;
    }

    private void check(Long id, Long userId, boolean citizen) {
        List<java.util.Map<String, Object>> rows = jdbc.queryForList(
                "select status, uploaded_by, application_number from mas_db.uploaded_documents where id = ?", id);
        if (rows.isEmpty() || !"ACTIVE".equalsIgnoreCase(String.valueOf(rows.get(0).get("status")))) {
            throw new IllegalArgumentException("Referenced document " + id + " does not exist");
        }
        if (!citizen) {
            return;
        }
        java.util.Map<String, Object> row = rows.get(0);
        Object uploadedBy = row.get("uploaded_by");
        if (userId != null && uploadedBy instanceof Number n && n.longValue() == userId) {
            return;
        }
        Object appNo = row.get("application_number");
        if (userId != null && appNo != null && !String.valueOf(appNo).isBlank()) {
            Integer owned = jdbc.queryForObject(
                    "select count(*) from mas_db.t_application_master where applicant_id = ? "
                            + "and (application_number = ? or application_no = ?)",
                    Integer.class, userId, String.valueOf(appNo), String.valueOf(appNo));
            if (owned != null && owned > 0) {
                return;
            }
        }
        throw new IllegalArgumentException("Referenced document " + id + " does not exist");
    }

    private void collect(Object o, List<Long> out, int depth) {
        if (o == null || depth > MAX_DEPTH) {
            return;
        }
        Class<?> c = o.getClass();
        if (!c.getName().startsWith("com.mas") && !c.getName().startsWith("com.MAS")) {
            return;
        }
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
            for (Field f : k.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    Object v = f.get(o);
                    if (v == null) {
                        continue;
                    }
                    if (DOC_FIELD.matcher(f.getName()).matches()) {
                        addIds(v, out);
                    } else if (v instanceof Collection<?> col) {
                        for (Object e : col) {
                            collect(e, out, depth + 1);
                        }
                    } else {
                        collect(v, out, depth + 1);
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // unreadable field: not a document reference we can validate
                }
            }
        }
    }

    private void addIds(Object v, List<Long> out) {
        if (v instanceof Number n) {
            if (n.longValue() > 0) out.add(n.longValue());
        } else if (v instanceof Collection<?> col) {
            col.forEach(e -> addIds(e, out));
        } else if (v instanceof CharSequence s) {
            for (String part : s.toString().split(",")) {
                String t = part.trim();
                if (t.length() > 0 && t.length() <= 18 && t.chars().allMatch(Character::isDigit) && Long.parseLong(t) > 0) out.add(Long.parseLong(t));
            }
        }
    }
}
