package com.laioffer.jupiter.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.laioffer.jupiter.entity.db.Item;

public class FavoriteRequestBody { //从request中拿来item info
    @JsonProperty("favorite")
    private Item favoriteItem;

    public Item getFavoriteItem(){
        return favoriteItem;
    }
}
