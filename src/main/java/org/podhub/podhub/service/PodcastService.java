package org.podhub.podhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.model.Podcast;
import org.podhub.podhub.repository.PodcastRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PodcastService {

    private final PodcastRepository podcastRepository;

    /**
     * Crea un nuevo podcast en la base de datos
     * Valida que el slug sea único y establece fechas automáticamente
     */
    public Podcast createPodcast(Podcast podcast) {
        log.info("=== CREATE PODCAST DEBUG START ===");
        log.info("Received podcast object: {}", podcast);
        log.info("Slug value: [{}]", podcast.getSlug());
        log.info("Slug is null: {}", podcast.getSlug() == null);
        log.info("Title: [{}]", podcast.getTitle());

        log.info("Checking existsBySlug for: [{}]", podcast.getSlug());
        boolean exists = podcastRepository.existsBySlug(podcast.getSlug());
        log.info("existsBySlug returned: {}", exists);

        if (exists) {
            log.error("Slug already exists! Throwing IllegalArgumentException");
            throw new IllegalArgumentException("Podcast with slug '" + podcast.getSlug() + "' already exists");
        }

        log.info("Setting timestamps...");
        Instant now = Instant.now();
        podcast.setCreatedAt(now);
        podcast.setUpdatedAt(now);

        if (podcast.getIsPublic() == null) {
            log.info("Setting isPublic to false");
            podcast.setIsPublic(false);
        }

        log.info("About to save podcast to MongoDB...");
        try {
            Podcast saved = podcastRepository.save(podcast);
            log.info("Podcast saved successfully with id: {}", saved.getId());
            log.info("=== CREATE PODCAST DEBUG END (SUCCESS) ===");
            return saved;
        } catch (Exception e) {
            log.error("=== CREATE PODCAST DEBUG END (ERROR) ===");
            log.error("Exception type: {}", e.getClass().getName());
            log.error("Exception message: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Busca un podcast por ID
     * Retorna Optional vacío si no existe
     */
    public Optional<Podcast> findById(String id) {
        log.debug("Finding podcast by id: {}", id);
        return podcastRepository.findById(id);
    }

    /**
     * Busca un podcast por slug (para URLs amigables)
     * Retorna Optional vacío si no existe
     */
    public Optional<Podcast> findBySlug(String slug) {
        log.debug("Finding podcast by slug: {}", slug);
        return podcastRepository.findBySlug(slug);
    }

    /**
     * Obtiene todos los podcasts con paginación cursor-based
     *
     * @param cursor Timestamp del último elemento (null para primera página)
     * @param limit  Número máximo de elementos a retornar
     * @return Respuesta paginada con cursor para siguiente página
     */
    public PaginatedResponse<Podcast> findAll(Instant cursor, int limit) {
        log.debug("Finding all podcasts with cursor: {} and limit: {}", cursor, limit);

        // Agregar 1 al límite para detectar si hay más elementos
        List<Podcast> podcasts;
        if (cursor == null) {
            podcasts = podcastRepository.findFirstPodcasts(limit + 1);
        } else {
            podcasts = podcastRepository.findNextPodcasts(cursor, limit + 1);
        }

        return buildPaginatedResponse(podcasts, limit);
    }

    /**
     * Obtiene los podcasts de un creador específico con paginación cursor-based
     */
    public PaginatedResponse<Podcast> findByCreatorId(String creatorId, Instant cursor, int limit) {
        log.debug("Finding podcasts by creator: {} with cursor: {} and limit: {}", creatorId, cursor, limit);

        List<Podcast> podcasts;
        if (cursor == null) {
            podcasts = podcastRepository.findFirstPodcastsByCreator(creatorId, limit + 1);
        } else {
            podcasts = podcastRepository.findNextPodcastsByCreator(creatorId, cursor, limit + 1);
        }

        return buildPaginatedResponse(podcasts, limit);
    }

    /**
     * Obtiene solo los podcasts públicos con paginación cursor-based
     */
    public PaginatedResponse<Podcast> findPublicPodcasts(Instant cursor, int limit) {
        log.debug("Finding public podcasts with cursor: {} and limit: {}", cursor, limit);

        List<Podcast> podcasts;
        if (cursor == null) {
            podcasts = podcastRepository.findFirstPublicPodcasts(limit + 1);
        } else {
            podcasts = podcastRepository.findNextPublicPodcasts(cursor, limit + 1);
        }

        return buildPaginatedResponse(podcasts, limit);
    }

    /**
     * Busca podcasts por título con paginación cursor-based
     */
    public PaginatedResponse<Podcast> searchByTitle(String title, Instant cursor, int limit) {
        log.debug("Searching podcasts by title: {} with cursor: {} and limit: {}", title, cursor, limit);

        List<Podcast> podcasts;
        if (cursor == null) {
            podcasts = podcastRepository.findFirstPodcastsByTitle(title, limit + 1);
        } else {
            podcasts = podcastRepository.findNextPodcastsByTitle(title, cursor, limit + 1);
        }

        return buildPaginatedResponse(podcasts, limit);
    }

    /**
     * Actualiza un podcast existente
     * Valida que exista y que el slug no esté en uso si cambió
     */
    public Podcast updatePodcast(String id, Podcast updatedPodcast) {
        log.debug("Updating podcast with id: {}", id);

        Podcast existingPodcast = podcastRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Podcast not found with id: " + id));

        // Verificar si el slug cambió y si ya existe
        if (!existingPodcast.getSlug().equals(updatedPodcast.getSlug())) {
            if (podcastRepository.existsBySlug(updatedPodcast.getSlug())) {
                throw new IllegalArgumentException("Podcast with slug '" + updatedPodcast.getSlug() + "' already exists");
            }
        }

        updatedPodcast.setId(id);
        updatedPodcast.setCreatedAt(existingPodcast.getCreatedAt());
        updatedPodcast.setUpdatedAt(Instant.now());

        Podcast saved = podcastRepository.save(updatedPodcast);
        log.info("Podcast updated successfully with id: {}", saved.getId());
        return saved;
    }

    /**
     * Elimina un podcast por ID
     * Valida que exista antes de borrar
     */
    public void deletePodcast(String id) {
        log.debug("Deleting podcast with id: {}", id);

        if (!podcastRepository.existsById(id)) {
            throw new IllegalArgumentException("Podcast not found with id: " + id);
        }

        podcastRepository.deleteById(id);
        log.info("Podcast deleted successfully with id: {}", id);
    }

    /**
     * Construye la respuesta paginada a partir de una lista de podcasts
     *
     * @param podcasts Lista con limit+1 elementos
     * @param limit    Límite real solicitado
     * @return PaginatedResponse con nextCursor si hay más elementos
     */
    private PaginatedResponse<Podcast> buildPaginatedResponse(List<Podcast> podcasts, int limit) {
        boolean hasMore = podcasts.size() > limit;

        // Si hay más elementos, solo retornamos los primeros 'limit'
        List<Podcast> data;
        if (hasMore) {
            data = podcasts.subList(0, limit);
        } else {
            data = podcasts;
        }

        // Calcular el nextCursor (createdAt del último elemento)
        String nextCursor = null;
        if (hasMore) {
            if (!data.isEmpty()) {
                nextCursor = data.get(data.size() - 1).getCreatedAt().toString();
            }
        }

        return PaginatedResponse.<Podcast>builder()
                .data(data)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .count(data.size())
                .build();
    }
}
