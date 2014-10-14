package mikhail.kalashnikov.shoppinglists;

import android.provider.BaseColumns;

public class Category implements BaseColumns{
	public static final String TABLE_NAME = "item";
    public static final String COLUMN_NAME = "name";

	private String name;
	private long id;

	public Category(String name, long id) {
		super();
		this.name = name;
		this.id = id;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "Category [name=" + name + ", id=" + id + "]";
	}
	
}
