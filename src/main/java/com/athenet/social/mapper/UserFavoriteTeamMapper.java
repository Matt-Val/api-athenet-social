package com.athenet.social.mapper;

import org.springframework.stereotype.Component;

import com.athenet.social.dto.response.FavoriteTeamResponse;
import com.athenet.social.model.UserFavoriteTeam;

@Component
public class UserFavoriteTeamMapper {

    public FavoriteTeamResponse toResponse(UserFavoriteTeam favorite) {
        return new FavoriteTeamResponse(
                favorite.getId(),
                favorite.getTeamId(),
                favorite.getTeamName(),
                favorite.getInstitutionId(),
                favorite.getInstitutionName(),
                favorite.getCreatedAt());
    }
}
