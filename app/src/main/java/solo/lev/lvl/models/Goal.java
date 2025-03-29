package solo.lev.lvl.models;

public class Goal {
    private String id;
    private String title;
    private String description;
    private long deadline;
    private boolean completed;
    private String photoUrl;

    public Goal() {
        this.id = String.valueOf(System.currentTimeMillis());
    }

    public Goal(String title, String description, long deadline) {
        this.id = String.valueOf(System.currentTimeMillis());
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.completed = false;
        this.photoUrl = null;
    }

    public Goal(String title, String description, long deadline, boolean completed, String photoUrl) {
        this.id = String.valueOf(System.currentTimeMillis());
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.completed = completed;
        this.photoUrl = photoUrl;
    }

    public String getId() {
        return id;
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

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getStatus() {
        return isCompleted() ? "Проверено" : "Ожидает проверки";
    }

    public String getText() {
        return title + (description != null && !description.isEmpty() ? "\n" + description : "");
    }
} 