package com.athenet.social.service;

import java.time.Instant;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.athenet.social.model.UserFavoriteTeam;
import com.athenet.social.model.UserProfile;
import com.athenet.social.repository.UserFavoriteTeamRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavoriteTeamService {

    private final UserFavoriteTeamRepository favorites;

    // created = false cuando el equipo ya era favorito; el controlador devuelve 200 en vez de 201.
    public record FavoriteResult(UserFavoriteTeam favorite, boolean created) {
    }

    public List<UserFavoriteTeam> list(UserProfile profile) {
        return favorites.findByUserProfileIdOrderByCreatedAtDesc(profile.getId());
    }

    public FavoriteResult add(UserProfile profile, Long teamId) {
        var existing = favorites.findByUserProfileIdAndTeamId(profile.getId(), teamId);
        if (existing.isPresent()) {
            return new FavoriteResult(existing.get(), false);
        }

        UserFavoriteTeam favorite = new UserFavoriteTeam();
        favorite.setUserProfileId(profile.getId());
        favorite.setTeamId(teamId);
        favorite.setCreatedAt(Instant.now());

        try {
            return new FavoriteResult(favorites.saveAndFlush(favorite), true);
        } catch (DataIntegrityViolationException ex) {
            return favorites.findByUserProfileIdAndTeamId(profile.getId(), teamId)
                    .map(found -> new FavoriteResult(found, false))
                    .orElseThrow(() -> ex);
        }
    }

    // Quitar algo que ya no esta no es un error: el estado final es el mismo.
    public void remove(UserProfile profile, Long teamId) {
        favorites.findByUserProfileIdAndTeamId(profile.getId(), teamId)
                .ifPresent(favorites::delete);
    }
}
