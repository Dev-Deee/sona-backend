package io.trysona.sona.preset;

import io.trysona.sona.common.Jsonb;
import io.trysona.sona.preset.dto.CreatePresetRequest;
import io.trysona.sona.preset.dto.PresetResponse;
import io.trysona.sona.preset.dto.UpdatePresetRequest;
import io.trysona.sona.team.TeamService;
import io.trysona.sona.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Presets are readable by their owner, members of their team, or anyone when public. Only the
 * owner can change or delete them.
 */
@Service
public class PresetService {

    private final PresetRepository presetRepository;
    private final UserRepository userRepository;
    private final TeamService teamService;

    public PresetService(PresetRepository presetRepository, UserRepository userRepository, TeamService teamService) {
        this.presetRepository = presetRepository;
        this.userRepository = userRepository;
        this.teamService = teamService;
    }

    @Transactional(readOnly = true)
    public List<PresetResponse> list(Long userId, String tab) {
        String filter = tab == null ? "" : tab.trim();
        return presetRepository.findAccessible(userId, filter).stream().map(PresetResponse::from).toList();
    }

    @Transactional
    public PresetResponse create(Long userId, CreatePresetRequest request) {
        Preset preset = new Preset();
        preset.setName(request.name().trim());
        preset.setCalculatorTab(request.calculatorTab());
        preset.setInputs(Jsonb.objectOrEmpty(request.inputs()));
        preset.setOwner(userRepository.getReferenceById(userId));
        if (request.teamUuid() != null) {
            preset.setTeam(teamService.loadEditableTeam(request.teamUuid(), userId));
        }
        preset.setPublicPreset(Boolean.TRUE.equals(request.isPublic()));
        return PresetResponse.from(presetRepository.saveAndFlush(preset));
    }

    @Transactional(readOnly = true)
    public PresetResponse get(Long userId, UUID uuid) {
        Preset preset = load(uuid);
        boolean teamMember = preset.getTeam() != null
                && teamService.roleOf(preset.getTeam().getId(), userId).isPresent();
        if (!isOwner(preset, userId) && !teamMember && !preset.isPublicPreset()) {
            throw new AccessDeniedException("You do not have access to this preset");
        }
        return PresetResponse.from(preset);
    }

    @Transactional
    public PresetResponse update(Long userId, UUID uuid, UpdatePresetRequest request) {
        Preset preset = loadOwned(uuid, userId);
        if (request != null) {
            if (request.name() != null) {
                preset.setName(request.name().trim());
            }
            if (request.calculatorTab() != null) {
                preset.setCalculatorTab(request.calculatorTab());
            }
            if (request.inputs() != null) {
                preset.setInputs(Jsonb.objectOrEmpty(request.inputs()));
            }
            if (request.isPublic() != null) {
                preset.setPublicPreset(request.isPublic());
            }
        }
        return PresetResponse.from(presetRepository.saveAndFlush(preset));
    }

    @Transactional
    public void delete(Long userId, UUID uuid) {
        presetRepository.delete(loadOwned(uuid, userId));
    }

    private Preset loadOwned(UUID uuid, Long userId) {
        Preset preset = load(uuid);
        if (!isOwner(preset, userId)) {
            throw new AccessDeniedException("Only the preset owner can change it");
        }
        return preset;
    }

    private static boolean isOwner(Preset preset, Long userId) {
        return preset.getOwner() != null && preset.getOwner().getId().equals(userId);
    }

    private Preset load(UUID uuid) {
        return presetRepository.findByUuid(uuid).orElseThrow(() -> new EntityNotFoundException("Preset not found"));
    }
}
