package com.laioffer.jupiter.service;

import com.laioffer.jupiter.dao.FavoriteDao;
import com.laioffer.jupiter.entity.db.Item;
import com.laioffer.jupiter.entity.db.ItemType;
import com.laioffer.jupiter.entity.response.Game;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RecommendationService {
    private static final int DEFAULT_GAME_LIMIT = 3; // 游戏总数
    private static final int DEFAULT_PER_GAME_RECOMMENDATION_LIMIT = 10; //每个游戏的推荐数
    private static final int DEFAULT_TOTAL_RECOMMENDATION_LIMIT = 20;

    @Autowired
    private GameService gameService;

    @Autowired
    private FavoriteDao favoriteDao;

    //如果没有注册过，没有favorite item，全新用户，则根据top game search,访问量最高的游戏
    private List<Item> recommendByTopGames(ItemType type, List<Game> topGames) throws RecommendationException{
        List<Item> recommendedItem = new ArrayList<>();
        for (Game game : topGames) {
            List<Item> items;

            try {
                items = gameService.searchByType(game.getId(), type, DEFAULT_PER_GAME_RECOMMENDATION_LIMIT);
            } catch (TwitchException e) {
                throw new RecommendationException("Failed to get recommendation result.");
            }

            for (Item item : items) {
                if (recommendedItem.size() == DEFAULT_TOTAL_RECOMMENDATION_LIMIT){
                    return recommendedItem;
                }
                recommendedItem.add(item);
            }

        }
        return recommendedItem;
    }


    public Map<String, List<Item>> recommendItemsByDefault() throws RecommendationException {
        Map<String, List<Item>> recommendedItemMap = new HashMap<>();
        List<Game> topGames;

        try {
            topGames = gameService.topGames(DEFAULT_GAME_LIMIT);
        } catch (TwitchException e) {
            throw new RecommendationException("Failed to get game data for recommendation");
        }

        for(ItemType type : ItemType.values()) {
            recommendedItemMap.put(type.toString(), recommendByTopGames(type,topGames));
        }
        return recommendedItemMap;
    }


    /**
     * 有历史数据，登陆过的人
     * stream: list of gameId
     * video: list of gameId
     * clip: list of gameId
     * 然后去重排序
     * 把不在集合的部分作为result返回
     */
    private List<Item> recommendByFavoriteHistory(
            Set<String> favoritedItemIds, List<String> favoritedGameIds, ItemType type) throws RecommendationException {
        Map<String, Long> favoriteGameIdByCount = new HashMap<>();
        for(String gameId : favoritedGameIds) {
            favoriteGameIdByCount.put(gameId, favoriteGameIdByCount.getOrDefault(gameId, 0L) + 1);
        }
        //sort the hashMap;
        List<Map.Entry<String, Long>> sortedFavoriteGameIdListByCount = new ArrayList<>(
                favoriteGameIdByCount.entrySet());
        sortedFavoriteGameIdListByCount.sort((Map.Entry<String, Long> e1, Map.Entry<String, Long> e2) -> Long
                .compare(e2.getValue(), e1.getValue()));
        // See also: https://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values

        //搜索
        if(sortedFavoriteGameIdListByCount.size()>DEFAULT_GAME_LIMIT) {
            sortedFavoriteGameIdListByCount = sortedFavoriteGameIdListByCount.subList(0,DEFAULT_GAME_LIMIT);
        }

        List<Item> recommendedItems = new ArrayList<>();
        for (Map.Entry<String, Long> favoritedGame : sortedFavoriteGameIdListByCount) {
            List<Item> items;
            try{
                items = gameService.searchByType(favoritedGame.getKey(), type, DEFAULT_PER_GAME_RECOMMENDATION_LIMIT);
            } catch (TwitchException e) {
                throw new RecommendationException("failed to get recommendation list");
            }

            for (Item item : items) {
                if (recommendedItems.size() == DEFAULT_TOTAL_RECOMMENDATION_LIMIT) {
                    return recommendedItems;
                }

                if (!favoritedItemIds.contains(item.getId())){
                    recommendedItems.add(item);
                }
            }

        }
        return recommendedItems;

    }

    public Map<String, List<Item>> recommendItemsByUser(String userId) throws RecommendationException {
        Map<String, List<Item>> recommendedItemMap = new HashMap<>();

        Set<String> favoriteItemIds;
        Map<String, List<String>> favoriteGameIds;

        favoriteItemIds = favoriteDao.getFavoriteItemIds(userId);
        favoriteGameIds = favoriteDao.getFavoriteGameIds(favoriteItemIds);

        for (Map.Entry<String, List<String>> entry : favoriteGameIds.entrySet()) {
            if (entry.getValue().size() == 0) { // 没有favorite 过item
                List<Game> topGames;
                try {
                    topGames = gameService.topGames(DEFAULT_GAME_LIMIT);
                } catch (TwitchException e) {
                    throw new RecommendationException("Failed to get game data for recommendation");
                }
                recommendedItemMap.put(entry.getKey(), recommendByTopGames(ItemType.valueOf(entry.getKey()), topGames)); //返回topgames,让数据丰富
            } else { //favorite 过item
                recommendedItemMap.put(entry.getKey(), recommendByFavoriteHistory(favoriteItemIds, entry.getValue(), ItemType.valueOf(entry.getKey())));
            }
        }
        return recommendedItemMap;
    }


}
