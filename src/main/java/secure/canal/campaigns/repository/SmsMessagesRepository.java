package secure.canal.campaigns.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import secure.canal.campaigns.entity.SmsMessages;
import secure.canal.campaigns.enumeration.MessageStatus;

public interface SmsMessagesRepository extends PagingAndSortingRepository<SmsMessages, Long>, ListCrudRepository<SmsMessages, Long>, JpaRepository<SmsMessages,Long> {
    int countAllByStatus(MessageStatus smsStatus);
    Page<SmsMessages> findByStatusContaining(String name, Pageable pageable);
}