package com.example.football.service.impl;

import com.example.football.models.dto.xml.ImportPlayerDTO;
import com.example.football.models.dto.xml.ImportPlayerRootDTO;
import com.example.football.models.entity.Player;
import com.example.football.models.entity.Stat;
import com.example.football.models.entity.Team;
import com.example.football.models.entity.Town;
import com.example.football.repository.PlayerRepository;
import com.example.football.repository.StatRepository;
import com.example.football.repository.TeamRepository;
import com.example.football.repository.TownRepository;
import com.example.football.service.PlayerService;
import com.example.football.util.XmlParser;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PlayerServiceImpl implements PlayerService {
    private final Path path = Path.of("src/main/resources/files/xml/players.xml");

    private final PlayerRepository playerRepository;
    private final TownRepository townRepository;
    private final TeamRepository teamRepository;
    private final StatRepository statRepository;
    private final XmlParser xmlParser;
    private final Validator validator;
    private final ModelMapper modelMapper;

    @Autowired
    public PlayerServiceImpl(PlayerRepository playerRepository, TownRepository townRepository, TeamRepository teamRepository, StatRepository statRepository, XmlParser xmlParser, Validator validator, ModelMapper modelMapper) {
        this.playerRepository = playerRepository;
        this.townRepository = townRepository;
        this.teamRepository = teamRepository;
        this.statRepository = statRepository;
        this.xmlParser = xmlParser;
        this.modelMapper = modelMapper;
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Override
    public boolean areImported() {
        return this.playerRepository.count() > 0;
    }

    @Override
    public String readPlayersFileContent() throws IOException {
        return Files.readString(path);
    }

    @Override
    public String importPlayers() throws JAXBException, FileNotFoundException {
        ImportPlayerRootDTO importPlayerRootDTO = this.xmlParser.fromFile(path.toAbsolutePath().toString(), ImportPlayerRootDTO.class);
        return importPlayerRootDTO.getPlayers().stream().map(this::importPlayer).collect(Collectors.joining("\n"));
    }

    private String importPlayer(ImportPlayerDTO dto) {
        Set<ConstraintViolation<ImportPlayerDTO>> errors = this.validator.validate(dto);

        if (!errors.isEmpty()) {
            return "Invalid Player";
        }

        Optional<Player> optPlayer = this.playerRepository.findByEmail(dto.getEmail());

        if (optPlayer.isPresent()) {
            return "Invalid Player";
        }

        Optional<Town> town = this.townRepository.findByName(dto.getTown().getName());
        Optional<Team> team = this.teamRepository.findByName(dto.getTeam().getName());
        Optional<Stat> stat = this.statRepository.findById(dto.getStat().getId());

        Player player = this.modelMapper.map(dto, Player.class);

        player.setStat(stat.get());
        player.setTown(town.get());
        player.setTeam(team.get());

        this.playerRepository.save(player);

        return String.format("Successfully imported Player %s %s - %s",player.getFirstName(), player.getLastName(), player.getPosition().toString());
    }

    @Override
    public String exportBestPlayers() {
        LocalDate before = LocalDate.of(1995, 1, 1);
        LocalDate after = LocalDate.of(2003, 1,1);

        List<Player> players = this.playerRepository
                .findByBirthDateBetweenOrderByStatShootingDescStatPassingDescStatEnduranceDescLastNameAsc(before, after);

        return players
                .stream()
                .map(Player::toString)
                .collect(Collectors.joining("\n"));
    }
}
