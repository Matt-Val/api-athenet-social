package com.athenet.social.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.athenet.social.model.UserFavoriteTeam;

public interface UserFavoriteTeamRepository extends JpaRepository<UserFavoriteTeam, Long>{

     List<UserFavoriteTeam> findByUserProfileIdOrderByCreatedAtDesc(Long userProfileId);
     
    Optional<UserFavoriteTeam> findByUserProfileIdAndTeamId(Long userProfileId, Long teamId);
}
