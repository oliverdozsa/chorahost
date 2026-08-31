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
    ChoreCardRepository choreCardRepository;

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

        templateRepository.persist(template);
        return template;
    }

    @Transactional
    public ChoreCard instantiateCard(UUID templateId, InstantiateCardRequest overrides, User requester) {
        ChoreCardTemplate template = templateRepository.findByIdOptional(templateId)
                .orElseThrow(() -> ApiException.notFound("Template not found: " + templateId));
        teamService.requireAdminOrTeamAdmin(template.teamId, requester);

        ChoreCard card = new ChoreCard();
        card.teamId = template.teamId;
        card.templateId = template.id;
        card.title = overrides != null && overrides.title() != null ? overrides.title() : template.title;
        card.description = overrides != null && overrides.description() != null
                ? overrides.description() : template.description;
        card.rarity = template.rarity;
        card.complexity = template.complexity;
        card.estimatedMinutes = overrides != null && overrides.estimatedMinutes() != null
                ? overrides.estimatedMinutes() : template.estimatedMinutes;
        card.xpReward = overrides != null && overrides.xpReward() != null ? overrides.xpReward() : template.xpReward;
        card.goldReward = overrides != null && overrides.goldReward() != null
                ? overrides.goldReward() : template.goldReward;
        card.itemDrop = overrides != null && overrides.itemDrop() != null ? overrides.itemDrop() : template.itemDrop;
        card.status = CardStatus.AVAILABLE;

        choreCardRepository.persist(card);
        return card;
    }
}
