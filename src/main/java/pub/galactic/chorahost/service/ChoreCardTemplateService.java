package pub.galactic.chorahost.service;

import java.util.List;
import java.util.UUID;

import pub.galactic.chorahost.dto.CreateChoreCardTemplateRequest;
import pub.galactic.chorahost.dto.InstantiateCardRequest;
import pub.galactic.chorahost.entity.ChoreCard;
import pub.galactic.chorahost.entity.ChoreCardTemplate;
import pub.galactic.chorahost.entity.User;
import pub.galactic.chorahost.enums.CardStatus;
import pub.galactic.chorahost.exception.ApiException;
import pub.galactic.chorahost.repository.ChoreCardRepository;
import pub.galactic.chorahost.repository.ChoreCardTemplateRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ChoreCardTemplateService {

    @Inject
    ChoreCardTemplateRepository templateRepository;

    @Inject
    TeamService teamService;

    public List<ChoreCardTemplate> getTemplatesForTeam(UUID teamId) {
        return templateRepository.findByTeam(teamId);
    }

    @Transactional
    public ChoreCardTemplate createTemplate(CreateChoreCardTemplateRequest request, User requester) {
        teamService.requireAdminOrTeamAdmin(request.teamId(), requester);

        ChoreCardTemplate template = new ChoreCardTemplate();
        template.teamId = request.teamId();
        template.title = request.title();
        template.description = request.description();
        template.rarity = request.rarity();
        template.complexity = request.complexity();
        template.estimatedMinutes = request.estimatedMinutes();
        template.xpReward = request.xpReward();
        template.goldReward = request.goldReward();
        template.itemDrop = request.itemDrop();
        template.imageUrl = request.imageUrl();

        templateRepository.persist(template);
        return template;
    }
}
