package mikhail.kalashnikov.shoppinglists;

import android.provider.BaseColumns;

public class ListItem implements BaseColumns{
	public static final String TABLE_NAME = "listitem";
    public static final String COLUMN_LIST_ID = "list_id";
    public static final String COLUMN_ITEM_ID = "item_id";
    public static final String COLUMN_QTY = "qty";
    public static final String COLUMN_DONE = "done";
    public static final int DONE = 1;
    public static final int NOT_DONE = 0;
    
    private long id;
    private long list_id;
    private String qty;
    private Item item; 
    private int isDone;
    
	public ListItem(long list_id, Item item, String qty) {
		super();
		this.list_id = list_id;
		this.item = item;
		this.item.useItem();
		this.qty = qty;
		this.isDone = 0;
	}
	
	public ListItem(long id, long list_id, Item item, String qty, int isDone) {
		super();
		this.id = id;
		this.list_id = list_id;
		this.item = item;
		if(item!=null){
			this.item.useItem();
		}
		this.qty = qty;
		this.isDone = isDone;
	}

	public void setId(long id){
		this.id=id;
	}
	public long getId() {
		return id;
	}
	public long getList_id() {
		return list_id;
	}
	public Item getItem() {
		return item;
	}
	public void setItem(Item item) {
		this.item= item ;
	}
	public String getQty() {
		return qty;
	}
	
	public int getIsDone() {
		return isDone;
	}
	public void setIsDone(int isDone) {
		this.isDone = isDone;
	}
	public void setQty(String qty) {
		this.qty = qty;
	}
	@Override
	public String toString() {
		return "ListItem [id=" + id + ", list_id=" + list_id + ", item="
				+ item + ", qty=" + qty + ", isDone=" + isDone + "]";
	}
    
    
}