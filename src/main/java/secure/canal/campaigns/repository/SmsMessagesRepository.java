package secure.canal.campaigns.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import secure.canal.campaigns.entity.SmsMessages;
import secure.canal.campaigns.enumeration.MessageStatus;

import java.util.List;

public interface SmsMessagesRepository extends PagingAndSortingRepository<SmsMessages, Long>, ListCrudRepository<SmsMessages, Long> {
    int countAllByStatus(String status);
    //Page<SmsMessages> findAllByCampaignsIdAndStatusContaining (String status,Long campaignId, Pageable pageable);
    @Query("SELECT s FROM SmsMessages s WHERE s.campaigns.id = :campaignId AND s.status LIKE %:status%")
    Page<SmsMessages> findAllByCampaignsIdAndStatusContaining(@Param("campaignId") Long campaignId, @Param("status") String status, Pageable pageable);

    Page<SmsMessages> findAllByCampaignsId(Long campaignId,Pageable pageable);

    List<SmsMessages> findAllByCampaignsId(Long campaignId);


}