package org.podhub.podhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.model.Podcast;
import org.podhub.podhub.repository.PodcastRepository;
import org.podhub.podhub.security.AuthenticationService;
import org.springframework.stereotype.Service;
import org.podhub.podhub.exception.ConflictException;
import org.podhub.podhub.exception.ForbiddenException;
import org.podhub.podhub.exception.ResourceNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PodcastService {

    private final PodcastRepository podcastRepository;
    private final AuthenticationService authenticationService;

    /**
     * Validates that the given user owns the specified podcast
     *
     * @param podcastId ID of the podcast to check
     * @param userId ID of the user attempting the operation
     * @throws ResourceNotFoundException if podcast doesn't exist
     * @throws ForbiddenException if user doesn't own the podcast
     */
    private void validateOwnership(String podcastId, String userId) {
        Podcast podcast = podcastRepository.findById(podcastId)
                .orElseThrow(() -> new ResourceNotFoundException("Podcast not found with id: " + podcastId));

        if (!podcast.getCreatorId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to modify this podcast");
        }
    }

    /**
     * Crea un nuevo podcast en la base de datos
     * Valida que el slug sea único y establece fechas automáticamente
     * Asigna el creador del podcast
     *
     * @param podcast Podcast a crear
     * @param creatorUserId ID del usuario que crea el podcast
     * @return Podcast creado
     */
    public Podcast createPodcast(Podcast podcast, String creatorUserId) {
        log.debug("Creating new podcast with title: {} by creator: {}", podcast.getTitle(), creatorUserId);

        if (podcastRepository.existsBySlug(podcast.getSlug())) {
            throw new ConflictException("Podcast with slug '" + podcast.getSlug() + "' already exists");
        }

        // Set creator ID (from authenticated user)
        podcast.setCreatorId(creatorUserId);

        Instant now = Instant.now();
        podcast.setCreatedAt(now);
        podcast.setUpdatedAt(now);

        if (podcast.getIsPublic() == null) {
            podcast.setIsPublic(false);
        }

        Podcast saved = podcastRepository.save(podcast);

        // Promote user to CREATOR on first podcast creation
        authenticationService.promoteToCreator(creatorUserId);

        log.info("Podcast created successfully with id: {} by creator: {}", saved.getId(), creatorUserId);
        return saved;
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
     * Busca un podcast por ID o slug
     * Intenta primero como ID, si no existe intenta como slug
     *
     * @param idOrSlug Identificador que puede ser ID o slug
     * @return Optional con el podcast si se encuentra
     */
    public Optional<Podcast> findByIdOrSlug(String idOrSlug) {
        log.debug("Finding podcast by ID or slug: {}", idOrSlug);
        Optional<Podcast> result = findById(idOrSlug);
        if (result.isEmpty()) {
            result = findBySlug(idOrSlug);
        }
        return result;
    }

    /**
     * Obtiene todos los podcasts con paginación cursor-based y filtros opcionales
     *
     * @param cursor Timestamp del último elemento (null para primera página)
     * @param limit  Número máximo de elementos a retornar
     * @param isPublic Filtro opcional por visibilidad pública
     * @param creatorId Filtro opcional por creador
     * @param title Filtro opcional por búsqueda en título
     * @return Respuesta paginada con cursor para siguiente página
     */
    public PaginatedResponse<Podcast> findAll(Instant cursor, int limit, Boolean isPublic, String creatorId, String title) {
        log.debug("Finding podcasts with cursor: {}, limit: {}, isPublic: {}, creatorId: {}, title: {}",
                  cursor, limit, isPublic, creatorId, title);

        List<Podcast> podcasts;

        // Determinar qué método del repository usar según los filtros
        if (title != null && !title.trim().isEmpty()) {
            // Búsqueda por título tiene prioridad
            if (cursor == null) {
                podcasts = podcastRepository.findFirstPodcastsByTitle(title, limit + 1);
            } else {
                podcasts = podcastRepository.findNextPodcastsByTitle(title, cursor, limit + 1);
            }
        } else if (creatorId != null && !creatorId.trim().isEmpty()) {
            // Filtro por creador
            if (cursor == null) {
                podcasts = podcastRepository.findFirstPodcastsByCreator(creatorId, limit + 1);
            } else {
                podcasts = podcastRepository.findNextPodcastsByCreator(creatorId, cursor, limit + 1);
            }
        } else if (Boolean.TRUE.equals(isPublic)) {
            // Filtro por públicos
            if (cursor == null) {
                podcasts = podcastRepository.findFirstPublicPodcasts(limit + 1);
            } else {
                podcasts = podcastRepository.findNextPublicPodcasts(cursor, limit + 1);
            }
        } else {
            // Sin filtros, todos los podcasts
            if (cursor == null) {
                podcasts = podcastRepository.findFirstPodcasts(limit + 1);
            } else {
                podcasts = podcastRepository.findNextPodcasts(cursor, limit + 1);
            }
        }

        return buildPaginatedResponse(podcasts, limit);
    }

    /**
     * @deprecated Use findAll(cursor, limit, isPublic, creatorId, title) instead
     */
    @Deprecated
    public PaginatedResponse<Podcast> findByCreatorId(String creatorId, Instant cursor, int limit) {
        return findAll(cursor, limit, null, creatorId, null);
    }

    /**
     * @deprecated Use findAll(cursor, limit, isPublic, creatorId, title) instead
     */
    @Deprecated
    public PaginatedResponse<Podcast> findPublicPodcasts(Instant cursor, int limit) {
        return findAll(cursor, limit, true, null, null);
    }

    /**
     * @deprecated Use findAll(cursor, limit, isPublic, creatorId, title) instead
     */
    @Deprecated
    public PaginatedResponse<Podcast> searchByTitle(String title, Instant cursor, int limit) {
        return findAll(cursor, limit, null, null, title);
    }

    /**
     * Actualiza un podcast existente
     * Valida que exista, que el usuario sea el creador, y que el slug no esté en uso si cambió
     *
     * @param id ID del podcast a actualizar
     * @param updatedPodcast Datos actualizados del podcast
     * @param userId ID del usuario que intenta actualizar
     * @return Podcast actualizado
     * @throws ForbiddenException si el usuario no es el creador
     */
    public Podcast updatePodcast(String id, Podcast updatedPodcast, String userId) {
        log.debug("Updating podcast with id: {} by user: {}", id, userId);

        // Verify ownership first
        validateOwnership(id, userId);

        Podcast existingPodcast = podcastRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Podcast not found with id: " + id));

        // Verificar si el slug cambió y si ya existe
        if (!existingPodcast.getSlug().equals(updatedPodcast.getSlug())) {
            if (podcastRepository.existsBySlug(updatedPodcast.getSlug())) {
                throw new ConflictException("Podcast with slug '" + updatedPodcast.getSlug() + "' already exists");
            }
        }

        updatedPodcast.setId(id);
        updatedPodcast.setCreatorId(existingPodcast.getCreatorId()); // Preserve creator
        updatedPodcast.setCreatedAt(existingPodcast.getCreatedAt());
        updatedPodcast.setUpdatedAt(Instant.now());

        Podcast saved = podcastRepository.save(updatedPodcast);
        log.info("Podcast updated successfully with id: {} by user: {}", saved.getId(), userId);
        return saved;
    }

    /**
     * Elimina un podcast por ID
     * Valida que exista y que el usuario sea el creador antes de borrar
     *
     * @param id ID del podcast a eliminar
     * @param userId ID del usuario que intenta eliminar
     * @throws ForbiddenException si el usuario no es el creador
     */
    public void deletePodcast(String id, String userId) {
        log.debug("Deleting podcast with id: {} by user: {}", id, userId);

        // Verify ownership first
        validateOwnership(id, userId);

        podcastRepository.deleteById(id);
        log.info("Podcast deleted successfully with id: {} by user: {}", id, userId);
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
