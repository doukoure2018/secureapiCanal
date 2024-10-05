package secure.canal.campaigns.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import secure.canal.campaigns.entity.Campaigns;

import java.util.List;

public interface CampaignsRepository extends JpaRepository<Campaigns,Long> {

    List<Campaigns> findAllByOrderByCreatedAtAsc();
}
