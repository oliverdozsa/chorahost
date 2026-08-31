package pub.galactic.chorahost.service;

import pub.galactic.chorahost.entity.User;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GamificationService {

    /**
     * Awards XP and gold to a user and resolves any level-up, handling XP
     * overflow into the next level. The threshold to reach the next level is
     * {@code currentLevel * 1000}.
     */
    public LevelUpResult awardRewards(User user, int xpReward, int goldReward) {
        user.gold += goldReward;

        int totalXp = user.currentXp + xpReward;
        boolean leveledUp = false;
        int nextLevelXp = user.level * 1000;

        while (totalXp >= nextLevelXp) {
            totalXp -= nextLevelXp;
            user.level += 1;
            leveledUp = true;
            nextLevelXp = user.level * 1000;
        }
        user.currentXp = totalXp;

        return new LevelUpResult(leveledUp, user.level);
    }

    public record LevelUpResult(boolean leveledUp, int newLevel) {
    }
}
