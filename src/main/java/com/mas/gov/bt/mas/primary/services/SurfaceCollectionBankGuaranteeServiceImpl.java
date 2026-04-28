package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.request.ResubmitBGRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.SubmitBGRequestDTO;
import com.mas.gov.bt.mas.primary.dto.response.BGInstructionViewResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.BGResponseDTO;
import com.mas.gov.bt.mas.primary.entity.SurfaceCollectionBankGuarantee;
import com.mas.gov.bt.mas.primary.repository.SurfaceCollectionBankGuaranteeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SurfaceCollectionBankGuaranteeServiceImpl
        implements SurfaceCollectionBankGuaranteeService {

    private final SurfaceCollectionBankGuaranteeRepository bgRepository;

    @Override
    public BGInstructionViewResponseDTO viewInstructions(Long promoterId) {

        SurfaceCollectionBankGuarantee bg = bgRepository.findByPromoterId(promoterId)
                .orElseThrow(() -> new RuntimeException("BG request not found"));

        return new BGInstructionViewResponseDTO(
                bg.getAuctionApplication().getId(),
                bg.getAuctionApplication().getApplicationNo(),
                bg.getBgInstruction()
        );
    }

    @Override
    public BGResponseDTO submitBG(
            Long promoterId,
            SubmitBGRequestDTO dto
    ) {

        SurfaceCollectionBankGuarantee bg = bgRepository.findByPromoterId(promoterId)
                .orElseThrow(() -> new RuntimeException("BG request not found"));

        bg.setBgFileId(dto.getBgFileId());
        bg.setStatus("SUBMITTED");
        bg.setSubmittedOn(LocalDateTime.now());

        bgRepository.save(bg);

        return map(bg);
    }

    @Override
    public BGResponseDTO resubmitBG(
            Long promoterId,
            ResubmitBGRequestDTO dto
    ) {

        SurfaceCollectionBankGuarantee bg = bgRepository.findByPromoterId(promoterId)
                .orElseThrow(() -> new RuntimeException("BG request not found"));

        bg.setBgFileId(dto.getBgFileId());
        bg.setStatus("RESUBMITTED");
        bg.setResubmittedOn(LocalDateTime.now());

        bgRepository.save(bg);

        return map(bg);
    }

    private BGResponseDTO map(SurfaceCollectionBankGuarantee bg) {
        return BGResponseDTO.builder()
                .id(bg.getId())
                .auctionId(bg.getAuctionApplication().getId())
                .bgFileId(bg.getBgFileId())
                .bgInstruction(bg.getBgInstruction())
                .status(bg.getStatus())
                .submittedOn(bg.getSubmittedOn())
                .resubmittedOn(bg.getResubmittedOn())
                .build();
    }
}