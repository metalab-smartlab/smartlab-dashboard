package at.metalab.smartlab.dashboard;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PanelSetup {
	private List<Room> rooms;
}
