package unisinos;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import unisinos.entity.Message;
import unisinos.enumeration.MessageStatus;
import unisinos.dto.MessageDTO;
import unisinos.service.SnsService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

@Path("/messages")
@RolesAllowed("USER")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MessageResource {

    @Inject
    SnsService snsService;

    @POST
    @Transactional
    public Response send(MessageDTO dto) {

        Message msg = new Message();
        msg.setId(UUID.randomUUID());
        msg.setText(dto.text());
        msg.setSentAt(LocalDateTime.now());
        msg.setUpdatedAt(LocalDateTime.now());
        msg.setStatus(MessageStatus.PENDING);
        msg.persist();

        var snsInstant = snsService.publish(msg.getId(), msg.getText());
        msg.setSentAt(LocalDateTime.ofInstant(snsInstant, ZoneId.systemDefault()));
        msg.setStatus(MessageStatus.SENT);
        msg.setUpdatedAt(LocalDateTime.now());
        msg.persist();

        return Response.ok(Map.of(
                "id", msg.getId(),
                "snsSentAtMillis", snsInstant.toEpochMilli()
        )).build();
    }
}
