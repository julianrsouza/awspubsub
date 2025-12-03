package unisinos.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import unisinos.entity.Message;

@ApplicationScoped
public class MessageRepository implements PanacheRepository<Message> {
    
}
