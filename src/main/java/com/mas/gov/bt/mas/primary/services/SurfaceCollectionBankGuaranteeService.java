package com.mas.gov.bt.mas.primary.services;


import com.mas.gov.bt.mas.primary.dto.request.ResubmitBGRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.SubmitBGRequestDTO;
import com.mas.gov.bt.mas.primary.dto.response.BGInstructionViewResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.BGResponseDTO;

public interface SurfaceCollectionBankGuaranteeService {

    BGInstructionViewResponseDTO viewInstructions(Long promoterId);

    BGResponseDTO submitBG(Long promoterId, SubmitBGRequestDTO dto);

    BGResponseDTO resubmitBG(Long promoterId, ResubmitBGRequestDTO dto);
}