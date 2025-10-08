package org.podhub.podhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.podhub.podhub.model.Podcast;
import org.podhub.podhub.repository.PodcastRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
        log.debug("Creating new podcast with title: {}", podcast.getTitle());

        if (podcastRepository.existsBySlug(podcast.getSlug())) {
            throw new IllegalArgumentException("Podcast with slug '" + podcast.getSlug() + "' already exists");
        }

        Instant now = Instant.now();
        podcast.setCreatedAt(now);
        podcast.setUpdatedAt(now);

        if (podcast.getIsPublic() == null) {
            podcast.setIsPublic(false);
        }

        Podcast saved = podcastRepository.save(podcast);
        log.info("Podcast created successfully with id: {}", saved.getId());
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
     * Obtiene todos los podcasts con paginación
     * Ejemplo: PageRequest.of(0, 10) = primera página, 10 elementos
     */
    public Page<Podcast> findAll(Pageable pageable) {
        log.debug("Finding all podcasts with pagination");
        return podcastRepository.findAll(pageable);
    }

    /**
     * Obtiene los podcasts de un creador específico con paginación
     * Útil para dashboard de usuario
     */
    public Page<Podcast> findByCreatorId(String creatorId, Pageable pageable) {
        log.debug("Finding podcasts by creator id: {} with pagination", creatorId);
        return podcastRepository.findByCreatorId(creatorId, pageable);
    }

    /**
     * Obtiene solo los podcasts públicos con paginación
     * Para catálogo público y página principal
     */
    public Page<Podcast> findPublicPodcasts(Pageable pageable) {
        log.debug("Finding all public podcasts with pagination");
        return podcastRepository.findByIsPublicTrue(pageable);
    }

    /**
     * Busca podcasts por título (case-insensitive) con paginación
     * Para implementar barra de búsqueda
     */
    public Page<Podcast> searchByTitle(String title, Pageable pageable) {
        log.debug("Searching podcasts by title containing: {}", title);
        return podcastRepository.findByTitleContainingIgnoreCase(title, pageable);
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
}
