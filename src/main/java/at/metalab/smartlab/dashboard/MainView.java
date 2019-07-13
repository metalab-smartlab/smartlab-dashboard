package at.metalab.smartlab.dashboard;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.InputStreamFactory;
import com.vaadin.flow.server.StreamResource;

@Route
public class MainView extends VerticalLayout {

	private static final long serialVersionUID = 1L;

	private HomeassistantService ha;

	private boolean dark = false;

	public MainView(HomeassistantService ha) throws IOException {
		this.ha = ha;

		Tabs tabs = new Tabs();
		Div pages = new Div();
		Map<Tab, Component> tabsToPages = new HashMap<>();

		List<Room> rooms = new ArrayList<>();
		rooms.addAll(panelSetup().getRooms());
		rooms.add(createRoomOther(this.ha));

		Div firstPage = null;
		for (Room room : rooms) {
			Tab tab = room.getTab();
			Div content = room.getContent();

			tabsToPages.put(tab, content);
			tabs.add(tab);
			pages.add(content);

			if (firstPage == null) {
				firstPage = content;
				firstPage.setVisible(true);
			}
		}

		Set<Component> pagesShown = new HashSet<>();
		pagesShown.add(firstPage);

		tabs.addSelectedChangeListener(event -> {
			pagesShown.forEach(page -> page.setVisible(false));
			pagesShown.clear();
			Component selectedPage = tabsToPages.get(tabs.getSelectedTab());
			selectedPage.setVisible(true);
			pagesShown.add(selectedPage);
		});

		HorizontalLayout actions = new HorizontalLayout(tabs);

		InputStreamFactory f = new InputStreamFactory() {

			private static final long serialVersionUID = 1L;

			@Override
			public InputStream createInputStream() {
				return Thread.currentThread().getContextClassLoader().getResourceAsStream("metalab-logo.png");
			}
		};

		Div header = new Div();
		Image i = new Image(//
				new StreamResource("metalab-logo.png", f), //
				"Metalab Logo");
		header.add(i);

		Button toggleDarkMode = new Button("Toggle Dark Mode", VaadinIcon.LIGHTBULB.create());
		toggleDarkMode.addClickListener(l -> {
			if (dark) {
				getUI().get().getPage().executeJavaScript("document.documentElement.setAttribute(\"theme\",\"light\")");
			} else {
				getUI().get().getPage().executeJavaScript("document.documentElement.setAttribute(\"theme\",\"dark\")");
			}
			dark = !dark;
		});
		header.add(toggleDarkMode);

		Html h = new Html("<div style=\"font-weight: bold\">Metalab Smartlab - Dashboard</div>");
		header.add(h);

		add(header, actions, pages);
	}

	private Room createRoomOther(HomeassistantService s) {
		Tab tab = new Tab("Other");
		Div content = new Div();

		Button shutdownBtn = new Button("Shutdown");
		shutdownBtn.addClickListener(l -> s.service("mqtt", "publish", "{ \"topic\": \"metalab/shutdown\" }"));
		content.add(shutdownBtn);

		content.add(" ");

		Button antishutdownBtn = new Button("Startup");
		antishutdownBtn.addClickListener(l -> s.service("mqtt", "publish", "{ \"topic\": \"metalab/startup\" }"));
		content.add(antishutdownBtn);
		content.setVisible(false);

		return Room.builder().tab(tab).content(content).build();
	}

	private List<Room> buildRooms(String json) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode tree = mapper.readTree(json);

		List<JsonNode> roomNodes = new ArrayList<>();
		tree.forEach(c -> {
			if (isRoom(c)) {
				roomNodes.add(c);
			}
		});

		List<Room> rooms = new ArrayList<>();

		for (JsonNode room : roomNodes) {
			Room.RoomBuilder b = Room.builder();

			Tab tab = createTab(room);

			b.tab(tab);
			b.displayName(tab.getLabel());
			b.content(createContent(room));

			rooms.add(b.build());
		}

		Collections.sort(rooms);

		return rooms;
	}

	private static Tab createTab(JsonNode room) {
		return new Tab(room.get("attributes").get("friendly_name").textValue());
	}

	private Div createContent(JsonNode room) {
		Div div = new Div();

		List<String> entities = getEntities(room);
		Collections.sort(entities);

		if (entities.size() > 1) {
			String groupEntityId = room.get("entity_id").textValue();
			div.add(buttonOnOff("All", //
					l -> ha.haTurn(groupEntityId, true), //
					l -> ha.haTurn(groupEntityId, false)));
			div.add(new Html("<br>"));
		}

		for (String entity : entities) {
			String friendlyName = entity.substring(entity.indexOf(".") + 1); // very friendly

			Div controls = buttonOnOff(friendlyName, //
					l -> ha.haTurn(entity, true), //
					l -> ha.haTurn(entity, false));

			if (hasRGB(entity)) {
				Input color = new Input();
				color.setId(UUID.randomUUID().toString());
				color.setType("color");
				color.addValueChangeListener(l -> {
					try {
						String colorStr = l.getValue();

						int r = Integer.valueOf(colorStr.substring(1, 3), 16);
						int g = Integer.valueOf(colorStr.substring(3, 5), 16);
						int b = Integer.valueOf(colorStr.substring(5, 7), 16);
						String body = String.format(//
								"{ \"entity_id\": \"%s\", \"rgb_color\" : [%d, %d, %d] }", //
								entity, r, g, b);

						ha.post("/services/light/turn_on", body);
					} catch (IOException ioException) {
						ioException.printStackTrace();
					}
				});
				controls.add(" ");
				controls.add(color);
			}

			div.add(controls);
		}

		div.setVisible(false);

		return div;
	}

	private boolean hasRGB(String entityId) {
		try {
			String state = ha.get("/states/" + entityId);

			return new ObjectMapper().readTree(state).findPath("attributes").get("rgb_color") != null;
		} catch (IOException ignore) {
			return false;
		}
	}

	private Div buttonOnOff(String label, //
			ComponentEventListener<ClickEvent<Button>> on, //
			ComponentEventListener<ClickEvent<Button>> off) {

		Button onBtn = new Button("ON");
		onBtn.addClickListener(on);

		Button offBtn = new Button("OFF");
		offBtn.addClickListener(off);

		Div d = new Div();
		d.add(label);
		d.add(" ");
		d.add(onBtn);
		d.add(" ");
		d.add(offBtn);

		return d;
	}

	private static boolean isRoom(JsonNode node) {
		return node.has("entity_id") && node.get("entity_id").textValue().startsWith("group.mp_");
	}

	private static List<String> getEntities(JsonNode room) {
		List<String> l = new ArrayList<>();

		JsonNode node = room.findPath("attributes").findPath("entity_id");
		node.forEach(c -> {
			l.add(c.asText());
		});

		return l;
	}

	private PanelSetup panelSetup() throws IOException {
		List<Room> rooms = new ArrayList<>();

		try {
			String groupStates = ha.get("/states");
			rooms.addAll(buildRooms(groupStates));
		} catch (IOException e) {
			e.printStackTrace();
		}

		PanelSetup panelSetup = PanelSetup.builder()//
				.rooms(rooms)//
				.build();

		return panelSetup;
	}

}