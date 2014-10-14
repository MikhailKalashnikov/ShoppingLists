package mikhail.kalashnikov.shoppinglists;

import android.provider.BaseColumns;

public class Item implements BaseColumns, Comparable<Item>{
	public static final String TABLE_NAME = "item";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_QTY_TYPE = "qty_type";
    public static final String COLUMN_CATEGORY = "category";
    public static final int QTY_TYPE_PIECE = 0;
    public static final int QTY_TYPE_KG = 1;
    public static final int QTY_TYPE_LT = 2;
    public static final int QTY_TYPE_PACK = 3;
    
    private long id;
    private String name;
    private String qty_type;
    private String category;
    private int isUsed = 0;
	public Item(long id, String name, String qty_type, String category) {
		super();
		this.id = id;
		this.name = name;
		this.qty_type = qty_type;
		this.category = category;
	}
	public Item(String name, String qty_type, String category) {
		super();
		this.name = name;
		this.qty_type = qty_type;
		this.category = category;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public String getQty_type() {
		return qty_type;
	}
	public String getCategory(){
		return category;
	}
	public void setCategory(String category){
		this.category = category;
	}
	
	public boolean isUsed() {
		return isUsed>0;
	}
	public void useItem() {
		this.isUsed++;
	}
	
	public void releaseItem() {
		this.isUsed--;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setQty_type(String qty_type) {
		this.qty_type = qty_type;
	}
	@Override
	public String toString() {
		return name;
		//"Item [id=" + id + ", name=" + name + ", qty_type=" + qty_type + "]";
	}
	public String toStringFull(){
		return "Item [id=" + id + ", name=" + name + ", qty_type=" + qty_type  
		 	+ ", category=" + category 
		 	+ ", isUsed=" + isUsed + "]";
	}
	@Override
	public int compareTo(Item another) {
		int res = this.name.compareToIgnoreCase(another.getName());
		if (res == 0){
			res = this.qty_type.compareTo(another.getQty_type());
		}
		if (res == 0){
			res = this.category.compareTo(another.getCategory());
		}
		return res;
	}
    
	
}
