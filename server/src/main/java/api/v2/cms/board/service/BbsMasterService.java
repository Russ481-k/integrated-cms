package api.v2.cms.board.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import api.v2.cms.board.dto.BbsMasterDto;

public interface BbsMasterService {
    BbsMasterDto createBbsMaster(BbsMasterDto bbsMasterDto);

    BbsMasterDto updateBbsMaster(Long bbsId, BbsMasterDto bbsMasterDto);

    void deleteBbsMaster(Long bbsId);

    BbsMasterDto getBbsMaster(Long bbsId);

    Page<BbsMasterDto> getBbsMasters(Pageable pageable);

    Page<BbsMasterDto> searchBbsMasters(String keyword, Pageable pageable);
}