package com.mas.gov.bt.mas.primary.services;


import com.mas.gov.bt.mas.primary.dto.request.ResubmitBGRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.SubmitBGRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.SurfaceCollectionAuctionListResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.BGInstructionViewResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.BGResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SurfaceCollectionBankGuaranteeService {

    BGInstructionViewResponseDTO viewInstructions(Long promoterId);

    BGResponseDTO submitBG(Long promoterId, SubmitBGRequestDTO dto);

    BGResponseDTO resubmitBG(Long promoterId, ResubmitBGRequestDTO dto);

    Page<SurfaceCollectionAuctionListResponseDTO> getMyApplications(String search, Pageable pageable, Long userId);

    Page<SurfaceCollectionAuctionListResponseDTO> getMyArchive(String search, Pageable pageable, Long userId);
}