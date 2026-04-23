package com.mentis.hrms.service;

import com.mentis.hrms.dto.AnnouncementDTO;
import com.mentis.hrms.model.Announcement;
import com.mentis.hrms.repository.AnnouncementRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AnnouncementService {

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public AnnouncementDTO createAnnouncement(AnnouncementDTO dto, HttpSession session) {
        Announcement announcement = new Announcement();
        announcement.setTitle(dto.getTitle());
        announcement.setContent(dto.getContent());
        announcement.setType(Announcement.AnnouncementType.valueOf(dto.getType()));
        announcement.setCategory(dto.getCategory());
        announcement.setPriority(Announcement.AnnouncementPriority.valueOf(dto.getPriority()));
        announcement.setTargetAudience(Announcement.TargetAudience.valueOf(dto.getTargetAudience()));
        announcement.setAnnouncementType(resolveAnnouncementType(dto.getCategory()));
        announcement.setPinned(dto.isPinned());
        announcement.setActive(true);
        announcement.setExpiresAt(dto.getExpiresAt());
        announcement.setCreatedBy((String) session.getAttribute("userId"));
        announcement.setCreatedByName((String) session.getAttribute("userName"));

        Announcement saved = announcementRepository.save(announcement);
        AnnouncementDTO savedDto = AnnouncementDTO.fromEntity(saved);
        broadcastAnnouncement(savedDto);
        return savedDto;
    }

    public List<AnnouncementDTO> getAnnouncementsForEmployee() {
        return announcementRepository.findByTargetAudience(
                        LocalDateTime.now(),
                        Announcement.AnnouncementType.TEMPORARY,
                        Announcement.TargetAudience.ALL,
                        Announcement.TargetAudience.EMPLOYEES_ONLY
                )
                .stream()
                .map(AnnouncementDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<AnnouncementDTO> getAllAnnouncementsForAdmin() {
        return announcementRepository.findAllForAdmin()
                .stream()
                .map(AnnouncementDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public boolean deleteAnnouncement(Long id) {
        if (!announcementRepository.existsById(id)) {
            return false;
        }
        announcementRepository.deleteById(id);
        return true;
    }

    public Optional<AnnouncementDTO> togglePin(Long id) {
        Optional<Announcement> announcementOpt = announcementRepository.findById(id);
        if (announcementOpt.isEmpty()) {
            return Optional.empty();
        }
        Announcement announcement = announcementOpt.get();
        announcement.setPinned(!announcement.isPinned());
        Announcement saved = announcementRepository.save(announcement);
        return Optional.of(AnnouncementDTO.fromEntity(saved));
    }

    public Optional<AnnouncementDTO> deactivateAnnouncement(Long id) {
        Optional<Announcement> announcementOpt = announcementRepository.findById(id);
        if (announcementOpt.isEmpty()) {
            return Optional.empty();
        }
        Announcement announcement = announcementOpt.get();
        announcement.setActive(false);
        Announcement saved = announcementRepository.save(announcement);
        return Optional.of(AnnouncementDTO.fromEntity(saved));
    }

    public int deactivateExpiredAnnouncements() {
        List<Announcement> expiredAnnouncements = announcementRepository.findExpiredTemporaryAnnouncements(
                LocalDateTime.now(),
                Announcement.AnnouncementType.TEMPORARY
        );
        expiredAnnouncements.forEach(a -> a.setActive(false));
        announcementRepository.saveAll(expiredAnnouncements);
        return expiredAnnouncements.size();
    }

    private void broadcastAnnouncement(AnnouncementDTO dto) {
        if ("ALL".equals(dto.getTargetAudience())) {
            messagingTemplate.convertAndSend("/topic/announcements/all", dto);
            messagingTemplate.convertAndSend("/topic/announcements/employees", dto);
            messagingTemplate.convertAndSend("/topic/announcements/hr", dto);
            messagingTemplate.convertAndSend("/topic/announcements", dto);
            return;
        }

        if ("EMPLOYEES_ONLY".equals(dto.getTargetAudience())) {
            messagingTemplate.convertAndSend("/topic/announcements/employees", dto);
            messagingTemplate.convertAndSend("/topic/announcements", dto);
            return;
        }

        if ("HR_ONLY".equals(dto.getTargetAudience())) {
            messagingTemplate.convertAndSend("/topic/announcements/hr", dto);
            messagingTemplate.convertAndSend("/topic/announcements", dto);
        }
    }

    private String resolveAnnouncementType(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "GENERAL";
        }
        String normalized = category.trim().toUpperCase().replace(' ', '_');
        return normalized.length() > 20 ? normalized.substring(0, 20) : normalized;
    }
}
