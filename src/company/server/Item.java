package company.server;

import javax.persistence.*;
@org.hibernate.annotations.NamedQueries({
        @org.hibernate.annotations.NamedQuery(
                name = "findItemByName",
                query = "SELECT item FROM Item item WHERE item.path LIKE :sPath"
        )
})
@Entity(name = "Item")
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long itemId;

    @Column(name = "path", nullable = false)
    String path = "root";
    @Column(name = "size", nullable = false)
    Integer size = 0;
    @Column(name = "access", nullable = false)
    String access = "private";
    @Column(name = "permissions", nullable = false)
    String permissions = "WR";

    protected Item(){}

    public Item(String name){
        this.path = name;
    }

    public Integer getSize() {
        return size;
    }

    public long getItemId() {
        return itemId;
    }

    public String getAccess() {
        return access;
    }

    public String getPath() {
        return path;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setAccess(String access) {
        this.access = access;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return (path + "\tsize " + size + "\taccess " + access + "\t" + permissions);
    }
}
