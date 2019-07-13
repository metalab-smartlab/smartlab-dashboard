package at.metalab.smartlab.dashboard;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ListRooms {

	@Autowired
	private HomeassistantService ha;

	public static void main(String[] args) {
		SpringApplication.run(ListRooms.class, args);
	}

	@PostConstruct
	public void exec() throws Exception {
		System.out.println();

		ObjectMapper mapper = new ObjectMapper();
		JsonNode actualObj = mapper.readTree(ha.get("/states"));

		List<JsonNode> rooms = new ArrayList<>();
		actualObj.forEach(c -> {
			if (isRoom(c)) {
				rooms.add(c);
			}
		});

		rooms.stream().forEach(c -> System.out.println(c.get("entity_id").asText()));

		rooms.stream().forEach(r -> {
			getEntities(r).forEach(c -> System.out.println(c));
		});

		System.exit(0);
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

}
