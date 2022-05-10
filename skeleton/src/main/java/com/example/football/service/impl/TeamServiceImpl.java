package com.example.football.service.impl;

import com.example.football.models.dto.ImportTeamDTO;
import com.example.football.models.entity.Team;
import com.example.football.models.entity.Town;
import com.example.football.repository.TeamRepository;
import com.example.football.repository.TownRepository;
import com.example.football.service.TeamService;
import com.google.gson.Gson;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final Gson gson;
    private final Validator validator;
    private final ModelMapper modelMapper;
    private final TownRepository townRepository;

    @Autowired
    public TeamServiceImpl(TeamRepository teamRepository, Gson gson, Validator validator, ModelMapper modelMapper, TownRepository townRepository) {
        this.teamRepository = teamRepository;
        this.townRepository = townRepository;
        this.gson = gson;
        this.modelMapper = modelMapper;
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Override
    public boolean areImported() {
        return this.teamRepository.count() > 0;
    }

    @Override
    public String readTeamsFileContent() throws IOException {
        Path path = Path.of("src/main/resources/files/json/teams.json");
        return Files.readString(path);
    }

    @Override
    public String importTeams() throws IOException {
        ImportTeamDTO[] importTeamDTOs = this.gson.fromJson(readTeamsFileContent(), ImportTeamDTO[].class);


        return Arrays.stream(importTeamDTOs)
                .map(this::importTeam)
                .collect(Collectors.joining("\n"));
    }

    private String importTeam(ImportTeamDTO dto) {
        Set<ConstraintViolation<ImportTeamDTO>> errors = this.validator.validate(dto);

        if (!errors.isEmpty()) {
            return "Invalid Team";
        }

        Optional<Team> optTeam = this.teamRepository.findByName(dto.getName());

        if (optTeam.isPresent()) {
            return "Invalid Team";
        }

        Optional<Town> town = this.townRepository.findByName(dto.getTownName());

        if (town.isEmpty()) {
            return "Invalid Team";
        }

        Team team = this.modelMapper.map(dto, Team.class);

        team.setTown(town.get());

        this.teamRepository.save(team);

        return String.format("Successfully imported Team %s - %d",team.getName(), team.getFanBase());
    }
}
