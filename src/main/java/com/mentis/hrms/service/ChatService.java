package com.mentis.hrms.service;

import com.mentis.hrms.dto.ChatRequestDto;
import com.mentis.hrms.dto.ChatResponseDto;
import com.mentis.hrms.model.ChatMessage;
import com.mentis.hrms.model.ChatSession;
import com.mentis.hrms.repository.ChatMessageRepository;
import com.mentis.hrms.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final int MAX_HISTORY_PAIRS = 10; // Keep last 10 exchanges in context

    @Autowired
    private ChatSessionRepository sessionRepository;

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private OpenRouterService openRouterService;

    // ── SEND A MESSAGE ──────────────────────────────────────────────
    public ChatResponseDto sendMessage(ChatRequestDto request) {
        ChatResponseDto response = new ChatResponseDto();

        try {
            String employeeContext = buildEmployeeContext(request.getEmployeeId());

            if (request.isTemporary()) {
                // TEMPORARY CHAT: Use client-provided history, don't save to DB
                List<OpenRouterService.MessagePair> history = buildHistoryFromClient(request.getHistory());
                String reply = openRouterService.chat(request.getMessage(), history, employeeContext);

                response.setSuccess(true);
                response.setReply(reply);
                response.setSessionId(-1L); // Signal: temporary session

            } else {
                // PERMANENT CHAT: Load/create session from DB
                ChatSession session = getOrCreateSession(request);

                // Load existing messages for context
                List<ChatMessage> existingMessages = messageRepository
                        .findBySessionIdOrderByCreatedAtAsc(session.getId());
                List<OpenRouterService.MessagePair> history = buildHistoryFromDB(existingMessages);

                // Get AI reply
                String reply = openRouterService.chat(request.getMessage(), history, employeeContext);

                // Save user message
                saveMessage(session, "user", request.getMessage());
                // Save assistant reply
                saveMessage(session, "assistant", reply);

                // Update session
                session.setUpdatedAt(LocalDateTime.now());
                if (session.getSessionName() == null || session.getSessionName().isEmpty()) {
                    session.setSessionName(generateSessionName(request.getMessage()));
                }
                sessionRepository.save(session);

                response.setSuccess(true);
                response.setReply(reply);
                response.setSessionId(session.getId());
                response.setSessionName(session.getSessionName());
            }

        } catch (Exception e) {
            log.error("Error in sendMessage: {}", e.getMessage(), e);
            response.setSuccess(false);
            response.setError("Failed to process message: " + e.getMessage());
        }

        return response;
    }

    // ── LOAD SESSION HISTORY ────────────────────────────────────────
    @Transactional(readOnly = true)
    public ChatResponseDto loadSessionHistory(Long sessionId, String employeeId) {
        ChatResponseDto response = new ChatResponseDto();
        try {
            ChatSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found"));

            // Security: ensure session belongs to this employee
            if (!session.getEmployeeId().equals(employeeId)) {
                response.setSuccess(false);
                response.setError("Unauthorized");
                return response;
            }

            List<ChatMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
            List<ChatResponseDto.MessageInfo> messageInfos = messages.stream()
                    .map(this::toMessageInfo)
                    .collect(Collectors.toList());

            response.setSuccess(true);
            response.setSessionId(sessionId);
            response.setSessionName(session.getSessionName());
            response.setMessages(messageInfos);

        } catch (Exception e) {
            log.error("Error loading session history: {}", e.getMessage(), e);
            response.setSuccess(false);
            response.setError(e.getMessage());
        }
        return response;
    }

    // ── GET ALL SESSIONS FOR EMPLOYEE ───────────────────────────────
    @Transactional(readOnly = true)
    public ChatResponseDto getAllSessions(String employeeId) {
        ChatResponseDto response = new ChatResponseDto();
        try {
            List<ChatSession> sessions = sessionRepository
                    .findByEmployeeIdAndIsTemporaryFalseOrderByUpdatedAtDesc(employeeId);

            List<ChatResponseDto.SessionInfo> sessionInfos = sessions.stream()
                    .map(s -> {
                        ChatResponseDto.SessionInfo info = new ChatResponseDto.SessionInfo();
                        info.setId(s.getId());
                        info.setSessionName(s.getSessionName() != null ? s.getSessionName() : "Chat " + s.getId());
                        info.setUpdatedAt(s.getUpdatedAt().format(FORMATTER));
                        info.setMessageCount(s.getMessages().size());
                        return info;
                    })
                    .collect(Collectors.toList());

            response.setSuccess(true);
            response.setSessions(sessionInfos);

        } catch (Exception e) {
            log.error("Error getting sessions: {}", e.getMessage(), e);
            response.setSuccess(false);
            response.setError(e.getMessage());
        }
        return response;
    }

    // ── DELETE SESSION ──────────────────────────────────────────────
    public ChatResponseDto deleteSession(Long sessionId, String employeeId) {
        ChatResponseDto response = new ChatResponseDto();
        try {
            ChatSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found"));

            if (!session.getEmployeeId().equals(employeeId)) {
                response.setSuccess(false);
                response.setError("Unauthorized");
                return response;
            }

            messageRepository.deleteBySessionId(sessionId);
            sessionRepository.delete(session);

            response.setSuccess(true);
        } catch (Exception e) {
            log.error("Error deleting session: {}", e.getMessage(), e);
            response.setSuccess(false);
            response.setError(e.getMessage());
        }
        return response;
    }

    // ── HELPERS ─────────────────────────────────────────────────────

    private ChatSession getOrCreateSession(ChatRequestDto request) {
        if (request.getSessionId() != null && request.getSessionId() > 0) {
            return sessionRepository.findById(request.getSessionId())
                    .orElseGet(() -> createNewSession(request.getEmployeeId(), false));
        }
        return createNewSession(request.getEmployeeId(), false);
    }

    private ChatSession createNewSession(String employeeId, boolean temporary) {
        ChatSession session = new ChatSession();
        session.setEmployeeId(employeeId);
        session.setIsTemporary(temporary);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    private void saveMessage(ChatSession session, String role, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setSession(session);
        msg.setRole(role);
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        messageRepository.save(msg);
    }

    private List<OpenRouterService.MessagePair> buildHistoryFromDB(List<ChatMessage> messages) {
        List<OpenRouterService.MessagePair> pairs = new ArrayList<>();
        String userMsg = null;

        // Only use last N messages for context window efficiency
        int start = Math.max(0, messages.size() - (MAX_HISTORY_PAIRS * 2));
        List<ChatMessage> recent = messages.subList(start, messages.size());

        for (ChatMessage msg : recent) {
            if ("user".equals(msg.getRole())) {
                userMsg = msg.getContent();
            } else if ("assistant".equals(msg.getRole()) && userMsg != null) {
                pairs.add(new OpenRouterService.MessagePair(userMsg, msg.getContent()));
                userMsg = null;
            }
        }
        return pairs;
    }

    private List<OpenRouterService.MessagePair> buildHistoryFromClient(
            List<ChatRequestDto.MessageDto> history) {

        List<OpenRouterService.MessagePair> pairs = new ArrayList<>();
        if (history == null || history.isEmpty()) return pairs;

        String userMsg = null;
        int start = Math.max(0, history.size() - (MAX_HISTORY_PAIRS * 2));

        for (int i = start; i < history.size(); i++) {
            ChatRequestDto.MessageDto msg = history.get(i);
            if ("user".equals(msg.getRole())) {
                userMsg = msg.getContent();
            } else if ("assistant".equals(msg.getRole()) && userMsg != null) {
                pairs.add(new OpenRouterService.MessagePair(userMsg, msg.getContent()));
                userMsg = null;
            }
        }
        return pairs;
    }

    private String buildEmployeeContext(String employeeId) {
        return "Employee ID: " + employeeId;
    }

    private String generateSessionName(String firstMessage) {
        if (firstMessage == null) return "New Chat";
        return firstMessage.length() > 50
                ? firstMessage.substring(0, 47) + "..."
                : firstMessage;
    }

    private ChatResponseDto.MessageInfo toMessageInfo(ChatMessage msg) {
        ChatResponseDto.MessageInfo info = new ChatResponseDto.MessageInfo();
        info.setId(msg.getId());
        info.setRole(msg.getRole());
        info.setContent(msg.getContent());
        info.setCreatedAt(msg.getCreatedAt().format(FORMATTER));
        return info;
    }
}