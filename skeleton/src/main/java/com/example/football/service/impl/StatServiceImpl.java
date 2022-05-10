package com.example.football.service.impl;

import com.example.football.models.dto.xml.ImportStatDTO;
import com.example.football.models.dto.xml.ImportStatRootDTO;
import com.example.football.models.entity.Stat;
import com.example.football.repository.StatRepository;
import com.example.football.service.StatService;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StatServiceImpl implements StatService {
    private final Path path = Path.of("src/main/resources/files/xml/stats.xml");

    private final StatRepository statRepository;
    private final XmlParser xmlParser;
    private final Validator validator;
    private final ModelMapper modelMapper;

    @Autowired
    public StatServiceImpl(StatRepository statRepository, XmlParser xmlParser, Validator validator, ModelMapper modelMapper) {
        this.statRepository = statRepository;
        this.xmlParser = xmlParser;
        this.modelMapper = modelMapper;
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Override
    public boolean areImported() {
        return this.statRepository.count() > 0;
    }

    @Override
    public String readStatsFileContent() throws IOException {
        return Files.readString(path);
    }

    @Override
    public String importStats() throws JAXBException, FileNotFoundException {
        ImportStatRootDTO importStatRootDTO = this.xmlParser.fromFile(path.toAbsolutePath().toString(), ImportStatRootDTO.class);
        return importStatRootDTO
                .getStats()
                .stream()
                .map(this::importStat)
                .collect(Collectors.joining("\n"));
    }

    private String importStat(ImportStatDTO dto) {
        Set<ConstraintViolation<ImportStatDTO>> errors = this.validator.validate(dto);

        if (!errors.isEmpty()) {
            return "Invalid Stat";
        }

        Optional<Stat> optionalStat = this.statRepository.findByPassingAndShootingAndEndurance(dto.getPassing(), dto.getShooting(), dto.getEndurance());

        if (optionalStat.isPresent()) {
            return "Invalid Stat";
        }

        Stat stat = this.modelMapper.map(dto, Stat.class);

        this.statRepository.save(stat);

        return String.format("Successfully imported Stat %.2f - %.2f - %.2f", stat.getShooting(), stat.getPassing(), stat.getEndurance());
    }
}
