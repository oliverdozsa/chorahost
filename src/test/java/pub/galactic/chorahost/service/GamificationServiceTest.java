package pub.galactic.chorahost.service;

import static org.assertj.core.api.Assertions.assertThat;

import pub.galactic.chorahost.entity.User;
import pub.galactic.chorahost.service.GamificationService.LevelUpResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GamificationServiceTest {

    private final GamificationService gamificationService = new GamificationService();
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.level = 1;
        user.currentXp = 0;
        user.gold = 0;
    }

    @Test
    void awardsGoldUnconditionally() {
        gamificationService.awardRewards(user, 0, 250);

        assertThat(user.gold).isEqualTo(250);
    }

    @Test
    void awardsXpWithoutCrossingThreshold_doesNotLevelUp() {
        LevelUpResult result = gamificationService.awardRewards(user, 400, 50);

        assertThat(user.currentXp).isEqualTo(400);
        assertThat(user.level).isEqualTo(1);
        assertThat(result.leveledUp()).isFalse();
        assertThat(result.newLevel()).isEqualTo(1);
    }

    @Test
    void xpExactlyAtThreshold_levelsUpWithZeroOverflow() {
        // level 1 -> next threshold is 1 * 1000
        LevelUpResult result = gamificationService.awardRewards(user, 1000, 0);

        assertThat(user.level).isEqualTo(2);
        assertThat(user.currentXp).isEqualTo(0);
        assertThat(result.leveledUp()).isTrue();
        assertThat(result.newLevel()).isEqualTo(2);
    }

    @Test
    void xpCrossingThreshold_levelsUpAndCarriesOverflow() {
        LevelUpResult result = gamificationService.awardRewards(user, 1200, 0);

        assertThat(user.level).isEqualTo(2);
        assertThat(user.currentXp).isEqualTo(200);
        assertThat(result.leveledUp()).isTrue();
        assertThat(result.newLevel()).isEqualTo(2);
    }

    @Test
    void xpCrossingMultipleThresholds_levelsUpRepeatedlyWithCorrectOverflow() {
        // level 1 -> 2 costs 1000, level 2 -> 3 costs 2000: total 3000 to reach level 3.
        LevelUpResult result = gamificationService.awardRewards(user, 3500, 0);

        assertThat(user.level).isEqualTo(3);
        assertThat(user.currentXp).isEqualTo(500);
        assertThat(result.leveledUp()).isTrue();
        assertThat(result.newLevel()).isEqualTo(3);
    }

    @Test
    void higherStartingLevel_usesScaledThreshold() {
        user.level = 5;
        user.currentXp = 4800;

        // next threshold at level 5 is 5 * 1000 = 5000, so 300 xp crosses it.
        LevelUpResult result = gamificationService.awardRewards(user, 300, 0);

        assertThat(user.level).isEqualTo(6);
        assertThat(user.currentXp).isEqualTo(100);
        assertThat(result.leveledUp()).isTrue();
    }
}
