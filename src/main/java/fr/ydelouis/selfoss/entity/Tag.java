package fr.ydelouis.selfoss.entity;

import android.content.Context;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Comparator;

import fr.ydelouis.selfoss.R;

@DatabaseTable
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tag implements Parcelable {

	public static final Tag ALL = new Tag(R.string.allTags);

	public static final Comparator<Tag> COMPARATOR_UNREAD_INVERSE = new Comparator<Tag>() {
		@Override
		public int compare(Tag lhs, Tag rhs) {
			return - Integer.valueOf(lhs.getUnread()).compareTo(rhs.getUnread());
		}
	};

	@DatabaseField(id = true)
	@JsonProperty("tag")
	private String name;
	private int nameId;
	@DatabaseField
	private int color;
	@DatabaseField
	private int unread;

	public Tag() {

	}

	private Tag(int nameId) {
		this.nameId = nameId;
	}

	public String getName(Context context) {
		if (nameId != 0)
			return context.getString(nameId);
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getColor() {
		return color;
	}

	@JsonProperty("color")
	public void setColor(String color) {
		this.color = Color.parseColor(color);
	}

	public int getUnread() {
		return unread;
	}

	public void setUnread(int unread) {
		this.unread = unread;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Tag))
			return false;
		Tag oTag = (Tag) o;
		if (nameId != 0)
			return nameId == oTag.nameId;
		return name.equals(oTag.name);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.name);
		dest.writeInt(this.nameId);
		dest.writeInt(this.color);
		dest.writeInt(this.unread);
	}

	private Tag(Parcel in) {
		this.name = in.readString();
		this.nameId = in.readInt();
		this.color = in.readInt();
		this.unread = in.readInt();
	}

	public static Parcelable.Creator<Tag> CREATOR = new Parcelable.Creator<Tag>() {
		public Tag createFromParcel(Parcel source) {
			return new Tag(source);
		}

		public Tag[] newArray(int size) {
			return new Tag[size];
		}
	};
}
