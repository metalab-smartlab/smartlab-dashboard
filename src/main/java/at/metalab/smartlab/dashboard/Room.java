package at.metalab.smartlab.dashboard;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.tabs.Tab;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Room implements Comparable<Room> {

	private String displayName;

	private Tab tab;

	private Div content;

	@Override
	public int compareTo(Room o) {
		return displayName.compareTo(o.displayName);
	}

}
