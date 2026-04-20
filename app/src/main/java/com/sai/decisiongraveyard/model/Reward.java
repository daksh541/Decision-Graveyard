package com.sai.decisiongraveyard.model;

public class Reward {

    private String rewardId;
    private String title;
    private String description;
    private int xpCost;
    private String icon;
    private String category;
    private boolean isAvailable;
    private boolean isOneTime;
    private long createdAt;
    private String rewardType;

    public Reward() {
        this.isAvailable = true;
        this.isOneTime = false;
        this.createdAt = System.currentTimeMillis();
    }

    public Reward(String rewardId, String title, String description, int xpCost, 
                  String icon, String category, String rewardType) {
        this();
        this.rewardId = rewardId;
        this.title = title;
        this.description = description;
        this.xpCost = xpCost;
        this.icon = icon;
        this.category = category;
        this.rewardType = rewardType;
    }

    public String getRewardId() {
        return rewardId;
    }

    public void setRewardId(String rewardId) {
        this.rewardId = rewardId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getXpCost() {
        return xpCost;
    }

    public void setXpCost(int xpCost) {
        this.xpCost = xpCost;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public boolean isOneTime() {
        return isOneTime;
    }

    public void setOneTime(boolean oneTime) {
        isOneTime = oneTime;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getRewardType() {
        return rewardType;
    }

    public void setRewardType(String rewardType) {
        this.rewardType = rewardType;
    }

    public static class Builder {
        private String rewardId;
        private String title;
        private String description;
        private int xpCost;
        private String icon;
        private String category;
        private String rewardType = "standard";
        private boolean isAvailable = true;
        private boolean isOneTime = false;
        private long createdAt;

        public Builder setRewardId(String rewardId) {
            this.rewardId = rewardId;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setXpCost(int xpCost) {
            this.xpCost = xpCost;
            return this;
        }

        public Builder setIcon(String icon) {
            this.icon = icon;
            return this;
        }

        public Builder setCategory(String category) {
            this.category = category;
            return this;
        }

        public Builder setRewardType(String rewardType) {
            this.rewardType = rewardType;
            return this;
        }

        public Builder setAvailable(boolean available) {
            isAvailable = available;
            return this;
        }

        public Builder setOneTime(boolean oneTime) {
            isOneTime = oneTime;
            return this;
        }

        public Builder setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Reward build() {
            Reward reward = new Reward(rewardId, title, description, xpCost, icon, category, rewardType);
            reward.setAvailable(isAvailable);
            reward.setOneTime(isOneTime);
            reward.setCreatedAt(createdAt);
            return reward;
        }
    }
}
