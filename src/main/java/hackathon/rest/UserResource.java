package hackathon.rest;

import hackathon.model.User;
import hackathon.model.UserRepository;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.persistence.NoResultException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@Path("/users")
public class UserResource {

    @Inject
    private UserRepository userRepository;

    @GET
    public String getAll() {
        final List<User> users = userRepository.getAll();

        return usersToJson(users).toString();
    }

    @GET
    @Path("/fb/{facebookId}")
    public String getUserByFacebookId(@PathParam("facebookId") String facebookId) {
        final User user = userRepository.getByFacebookId(facebookId);

        return userToJson(user).toString();
    }

    @POST
    public String addUser(String body) {
        User user = jsonToUser(body);

        try {
            user = userRepository.getByFacebookId(user.getFacebookId());
            if (!user.getRegistered()) {
                user.setRegistered(true);
                userRepository.save(user);
            }
        } catch (final NoResultException e) {
            userRepository.save(user);
        }

        return userToJson(user).toString();
    }

    @GET
    @Path("/{userId}/friends")
    public Response getFriends(@PathParam("userId") Long userId) {
        final User user = userRepository.get(userId);

        if (user == null) {
            return Response.status(404).build();
        }

        final List<User> friends = user.getFriends();

        return Response.status(200).entity(usersToJson(friends).toString()).build();
    }

    @POST
    @Path("/{userId}/friends")
    public void addFriends(final String body) {
        final List<User> users = fbJsonToUsers(body);

        for (final User user : users) {
            try {
                userRepository.getByFacebookId(user.getFacebookId());
            } catch (final NoResultException e) {
                userRepository.save(user);
            }
        }
    }

    private JsonArray usersToJson(final List<User> users) {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        for (final User user : users) {
            arrayBuilder.add(userToJson(user));
        }

        return arrayBuilder.build();
    }

    private JsonObject userToJson(final User user) {
        return Json.createObjectBuilder()
                .add("id", user.getId())
                .add("name", user.getName())
                .add("facebook_id", user.getFacebookId())
                .add("photo", user.getPhoto())
                .add("registered", user.getRegistered())
                .build();
    }

    private User jsonToUser(final String json) {
        JsonObject jsonObject = Json.createReader(new StringReader(json)).readObject();

        final User user = new User();
        user.setName(jsonObject.getString("name"));
        user.setFacebookId(jsonObject.getString("facebook_id"));
        user.setPhoto(jsonObject.getString("photo"));
        user.setRegistered(jsonObject.getBoolean("registered"));

        return user;
    }

    private List<User> fbJsonToUsers(final String json) {
        final List<User> users = new ArrayList<>();
        JsonArray jsonArray = Json.createReader(new StringReader(json)).readArray();

        for (int i = 0; i < jsonArray.size(); i++) {
            final JsonObject jsonObject = jsonArray.getJsonObject(i);
            final User user = new User();
            System.out.println(jsonObject.getString("id"));
            user.setName(jsonObject.getString("name"));
            user.setFacebookId(jsonObject.getString("id"));
            user.setPhoto("https://graph.facebook.com/" + user.getFacebookId() + "/picture");
            user.setRegistered(false);

            users.add(user);
        }

        return users;
    }
}
