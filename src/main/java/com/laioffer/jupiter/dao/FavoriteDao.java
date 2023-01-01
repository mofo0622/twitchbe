package com.laioffer.jupiter.dao;

import com.laioffer.jupiter.entity.db.Item;
import com.laioffer.jupiter.entity.db.ItemType;
import com.laioffer.jupiter.entity.db.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class FavoriteDao {
    @Autowired
    private SessionFactory sessionFactory;

    public void setFavoriteItem(String userId, Item item){
        Session session = null;

        try{
            session = sessionFactory.openSession();
            User user = session.get(User.class, userId); //找userID
            user.getItemSet().add(item);

            session.beginTransaction(); // 开启一个事务
            session.save(user);
            session.getTransaction().commit();

        } catch(Exception ex){
            ex.printStackTrace();
            if (session !=null) session.getTransaction().rollback();
        } finally {
            if (session!=null) session.close();
        }

    }

    public void unsetFavoriteItem(String userId, String itemId){ //删掉relationship
        Session session = null;
        try{
            session = sessionFactory.openSession();
            User user = session.get(User.class, userId);
            Item item = session.get(Item.class, itemId);
            user.getItemSet().remove(item);

            session.beginTransaction();
            session.update(user); // update
            session.getTransaction().commit();

        } catch(Exception ex){
            ex.printStackTrace();
            if (session !=null) session.getTransaction().rollback();
        } finally {
            if (session!=null) session.close();
        }

    }

    //get favorite item ids for the given user （已知favorite list，为分来做准备）
    public Set<String> getFavoriteItemIds(String userId) {
        Set<String> itemIds = new HashSet<>();

        try (Session session = sessionFactory.openSession()) {
            Set<Item> items = session.get(User.class, userId).getItemSet();
            for(Item item : items) {
                itemIds.add(item.getId());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return itemIds;
    }


    //根据id进行steam, clip, video分类（为排序去重做准备）
    public Map<String, List<String>> getFavoriteGameIds(Set<String> favoriteItemIds) {
        Map<String, List<String>> itemMap = new HashMap<>();
        for (ItemType type : ItemType.values()) {
            itemMap.put(type.toString(), new ArrayList<>());
        }

        try (Session session = sessionFactory.openSession()) {
            for(String itemId : favoriteItemIds) {
                Item item = session.get(Item.class, itemId);
                itemMap.get(item.getType().toString()).add(item.getGameId());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return itemMap;
    }




    /**
    public Set<Item> getFavoriteItems(String userId){
        Session session = null;
        try{
            session = sessionFactory.openSession();
            User user = session.get(User.class, userId);
            if (user != null){
                return user.getItemSet();
            }

        } catch (Exception ex){
            ex.printStackTrace();

        } finally{

        }

    }
     */

    public Set<Item> getFavoriteItems(String userId) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(User.class, userId).getItemSet();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new HashSet<>();
    }

}
