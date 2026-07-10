package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Permission Entity as per Section 34.7
 * Represents atomic access to system resources
 * Fully dynamic - permissions can be created through API
 */
@Entity
@Table(name = "permissions", schema = "mas_db",
       uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"roles"})
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String name; // Display name

    @Column(length = 500)
    private String description;

    @Column(name = "module", length = 100)
    private String module; // Module/category (e.g., "User Management", "Application", "Reports")

    @Column(name = "resource", length = 100)
    private String resource; // Resource type (e.g., "User", "Application", "Document")

    @Column(name = "action", length = 50)
    private String action; // Action type (e.g., "createUnitOfMeasurement", "read", "updateTypeOfMine", "delete", "approve")

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "menu_label", length = 100)
    private String menuLabel; // Label to display in menu

    @Column(name = "menu_path", length = 200)
    private String menuPath; // Frontend route path (e.g., "/users", "/applications")

    @Column(name = "menu_icon", length = 50)
    private String menuIcon; // Icon class/name for UI

    @Column(name = "menu_order")
    private Integer menuOrder; // Display order in menu

    @Column(name = "parent_menu_id")
    private Long parentMenuId; // For submenu structure

    @Column(name = "is_menu_item")
    private Boolean isMenuItem = false; // Whether this shows in menu

    @Column(name = "type", length = 50)
    private String type; // Menu type: "item" (clickable) or "collapse" (parent with children)

    @Column(name = "grants_child_access")
    private Boolean grantsChildAccess = false; // If true, grants access to all child menus/actions

    /**
     * Citizen permission level gate.
     * A citizen at level N can see all permissions where permission_level <= N.
     * Default 1000 = hidden from brand-new (level 1) promotors; a permission must
     * be explicitly lowered (e.g. to 1 via the New Promotor set) to be visible to them.
     */
    @Column(name = "permission_level", nullable = false, columnDefinition = "int default 1000")
    private Integer permissionLevel = 1000;

    /**
     * Which audience this row belongs to: PROMOTOR (citizen menu, gated by
     * permissionLevel + PermissionDependency), AGENCY (staff menu/action,
     * gated by role_permissions), or BOTH (single row serves both — needs
     * both gates populated). Default AGENCY preserves existing behavior for
     * every row that predates this column.
     */
    @Column(name = "user_type", nullable = false, length = 10, columnDefinition = "varchar(10) default 'AGENCY'")
    private String userType = "AGENCY";

    // Many-to-Many relationship with Role
    @ManyToMany(mappedBy = "permissions")
    private Set<Role> roles = new HashSet<>();

    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}
