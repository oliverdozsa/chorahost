package pub.galactic.chorahost.service;

import pub.galactic.chorahost.entity.TeamMembership;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GamificationService {

    /**
     * Awards XP and gold to a team membership and resolves any level-up,
     * handling XP overflow into the next level. Level/XP/gold are tracked
     * per team membership, not globally on the user. The threshold to reach
     * the next level is {@code currentLevel * 1000}.
     */
    public LevelUpResult awardRewards(TeamMembership membership, int xpReward, int goldReward) {
        membership.gold += goldReward;

        int totalXp = membership.currentXp + xpReward;
        boolean leveledUp = false;
        int nextLevelXp = membership.level * 1000;

        while (totalXp >= nextLevelXp) {
            totalXp -= nextLevelXp;
            membership.level += 1;
            leveledUp = true;
            nextLevelXp = membership.level * 1000;
        }
        membership.currentXp = totalXp;

        return new LevelUpResult(leveledUp, membership.level);
    }

    public record LevelUpResult(boolean leveledUp, int newLevel) {
    }
}
