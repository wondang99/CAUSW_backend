package net.causw.application;

import lombok.NoArgsConstructor;
import net.causw.application.spi.FlagPort;
import net.causw.application.spi.LockerLogPort;
import net.causw.application.spi.LockerPort;
import net.causw.application.spi.TextFieldPort;
import net.causw.domain.exceptions.ErrorCode;
import net.causw.domain.exceptions.InternalServerException;
import net.causw.domain.model.LockerDomainModel;
import net.causw.domain.model.LockerLogAction;
import net.causw.domain.model.Role;
import net.causw.domain.model.StaticValue;
import net.causw.domain.model.UserDomainModel;
import net.causw.domain.validation.LockerAccessValidator;
import net.causw.domain.validation.LockerInUseValidator;
import net.causw.domain.validation.LockerIsDeactivatedValidator;
import net.causw.domain.validation.TimePassedValidator;
import net.causw.domain.validation.ValidatorBucket;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static net.causw.domain.model.StaticValue.LOCKER_ACCESS;

@NoArgsConstructor
public class LockerActionRegister implements LockerAction {
    @Override
    public Optional<LockerDomainModel> updateLockerDomainModel(
            LockerDomainModel lockerDomainModel,
            UserDomainModel updaterDomainModel,
            LockerPort lockerPort,
            LockerLogPort lockerLogPort,
            FlagPort flagPort,
            TextFieldPort textFieldPort
    ) {
        ValidatorBucket.of()
                .consistOf(LockerInUseValidator.of(lockerDomainModel.getUser().isPresent()))
                .consistOf(LockerIsDeactivatedValidator.of(lockerDomainModel.getIsActive()))
                .validate();

        if (!updaterDomainModel.getRole().equals(Role.ADMIN)) {
            ValidatorBucket.of()
                    .consistOf(LockerAccessValidator.of(flagPort.findByKey(LOCKER_ACCESS).orElse(false)))
                    .validate();

            lockerLogPort.whenRegister(updaterDomainModel).ifPresent(
                    createdAt -> ValidatorBucket.of()
                            .consistOf(TimePassedValidator.of(createdAt))
                            .validate()
            );

            lockerPort.findByUserId(updaterDomainModel.getId()).ifPresent(locker -> {
                locker.returnLocker();
                lockerPort.update(
                        locker.getId(),
                        locker
                );

                lockerLogPort.create(
                        locker.getLockerNumber(),
                        locker.getLockerLocation().getName(),
                        updaterDomainModel,
                        LockerLogAction.RETURN,
                        ""
                );
            });
        }

        lockerDomainModel.register(
                updaterDomainModel,
                LocalDateTime.parse(textFieldPort.findByKey(StaticValue.EXPIRED_AT).orElseThrow(
                        () -> new InternalServerException(
                                ErrorCode.INTERNAL_SERVER,
                                "사물함 반납 기한을 설정하지 않았습니다."
                        )
                ), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );

        return lockerPort.update(
                lockerDomainModel.getId(),
                lockerDomainModel
        );
    }
}
