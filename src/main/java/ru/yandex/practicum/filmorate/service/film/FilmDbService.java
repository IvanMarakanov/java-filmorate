package ru.yandex.practicum.filmorate.service.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.storage.dao.genre.GenreNotFoundException;
import ru.yandex.practicum.filmorate.exception.storage.dao.mpa.MpaNotFoundException;
import ru.yandex.practicum.filmorate.exception.storage.film.FilmAlreadyExistsException;
import ru.yandex.practicum.filmorate.exception.storage.film.FilmNotFoundException;
import ru.yandex.practicum.filmorate.exception.storage.film.LikeAlreadyExistsException;
import ru.yandex.practicum.filmorate.exception.storage.film.LikeNotFoundException;
import ru.yandex.practicum.filmorate.exception.storage.user.UserNotFoundException;
import ru.yandex.practicum.filmorate.model.film.Film;
import ru.yandex.practicum.filmorate.model.film.Genre;
import ru.yandex.practicum.filmorate.storage.dao.genre.GenreDao;
import ru.yandex.practicum.filmorate.storage.dao.like.LikeDao;
import ru.yandex.practicum.filmorate.storage.dao.mpa.MpaDao;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.yandex.practicum.filmorate.exception.storage.dao.genre.GenreNotFoundException.GENRE_NOT_FOUND;
import static ru.yandex.practicum.filmorate.exception.storage.dao.mpa.MpaNotFoundException.MPA_NOT_FOUND;
import static ru.yandex.practicum.filmorate.exception.storage.film.FilmAlreadyExistsException.FILM_ALREADY_EXISTS;
import static ru.yandex.practicum.filmorate.exception.storage.film.FilmNotFoundException.FILM_NOT_FOUND;
import static ru.yandex.practicum.filmorate.exception.storage.film.LikeAlreadyExistsException.LIKE_ALREADY_EXISTS;
import static ru.yandex.practicum.filmorate.exception.storage.film.LikeNotFoundException.LIKE_NOT_FOUND;
import static ru.yandex.practicum.filmorate.exception.storage.user.UserNotFoundException.USER_NOT_FOUND;

@Slf4j
@Service("FilmDbService")
public class FilmDbService implements FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final GenreDao genreDao;
    private final LikeDao likeDao;
    private final MpaDao mpaDao;

    @Autowired
    public FilmDbService(@Qualifier("FilmDbStorage") FilmStorage filmStorage,
                         @Qualifier("UserDbStorage") UserStorage userStorage,
                         GenreDao genreDao,
                         LikeDao likeDao,
                         MpaDao mpaDao) {
        log.debug("FilmDbService({}, {}, {}, {}, {}).",
                filmStorage.getClass().getSimpleName(),
                userStorage.getClass().getSimpleName(),
                genreDao.getClass().getSimpleName(),
                likeDao.getClass().getSimpleName(),
                mpaDao.getClass().getSimpleName());
        this.filmStorage = filmStorage;
        log.info(DEPENDENCY_MESSAGE, filmStorage.getClass().getName());
        this.userStorage = userStorage;
        log.info(DEPENDENCY_MESSAGE, userStorage.getClass().getName());
        this.genreDao = genreDao;
        log.info(DEPENDENCY_MESSAGE, genreDao.getClass().getName());
        this.likeDao = likeDao;
        log.info(DEPENDENCY_MESSAGE, likeDao.getClass().getName());
        this.mpaDao = mpaDao;
        log.info(DEPENDENCY_MESSAGE, mpaDao.getClass().getName());
    }

    @Override
    public Film add(Film film) {
        if (film.getId() != 0) {
            if (filmStorage.contains(film.getId())) {
                log.warn("Не удалось добавить фильм: {}.", format(FILM_ALREADY_EXISTS, film.getId()));
                throw new FilmAlreadyExistsException(format(FILM_ALREADY_EXISTS, film.getId()));
            } else {
                log.warn("Не удалось добавить фильм: {}.", "Запрещено устанавливать ID вручную");
                throw new IllegalArgumentException("Запрещено устанавливать ID вручную");
            }
        }
        for (Genre genre : film.getGenres()) {
            if (!genreDao.contains(genre.getId())) {
                log.warn("Фильму ID_{} не удалось добавить жанр: {}.",
                        film.getId(), format(GENRE_NOT_FOUND, genre.getId()));
                throw new GenreNotFoundException(format(GENRE_NOT_FOUND, genre.getId()));
            }
        }
        Film result = filmStorage.add(film);
        filmStorage.addGenres(result.getId(), film.getGenres());
        result.setGenres(filmStorage.getGenres(result.getId()));
        return result;
    }

    @Override
    public Film update(Film film) {
        if (!filmStorage.contains(film.getId())) {
            log.warn("Не удалось обновить фильм: {}.", format(FILM_NOT_FOUND, film.getId()));
            throw new FilmNotFoundException(format(FILM_NOT_FOUND, film.getId()));
        }
        if (!mpaDao.contains(film.getMpa().getId())) {
            log.warn("Не удалось вернуть рейтинг MPA: {}.",
                    format(MPA_NOT_FOUND, film.getMpa().getId()));
            throw new MpaNotFoundException(format(MPA_NOT_FOUND, film.getMpa().getId()));
        }
        Film result = filmStorage.update(film);
        filmStorage.updateGenres(result.getId(), film.getGenres());
        result.setGenres(filmStorage.getGenres(result.getId()));
        result.setMpa(mpaDao.get(result.getMpa().getId()));
        return result;
    }

    @Override
    public Film get(long filmID) {
        if (!filmStorage.contains(filmID)) {
            log.warn("Не удалось вернуть фильм: {}.", format(FILM_NOT_FOUND, filmID));
            throw new FilmNotFoundException(format(FILM_NOT_FOUND, filmID));
        }
        Film film = filmStorage.get(filmID);
        film.setGenres(filmStorage.getGenres(filmID));
        if (!mpaDao.contains(film.getMpa().getId())) {
            log.warn("Не удалось вернуть рейтинг MPA: {}.",
                    format(MPA_NOT_FOUND, film.getMpa().getId()));
            throw new MpaNotFoundException(format(MPA_NOT_FOUND, film.getMpa().getId()));
        }
        film.setMpa(mpaDao.get(film.getMpa().getId()));
        return film;
    }

    @Override
    public Collection<Film> getAll() {
        var films = filmStorage.getAll();
        for (Film film : films) {
            film.setGenres(filmStorage.getGenres(film.getId()));
            if (!mpaDao.contains(film.getMpa().getId())) {
                log.warn("Не удалось вернуть рейтинг MPA: {}.",
                        format(MPA_NOT_FOUND, film.getMpa().getId()));
                throw new MpaNotFoundException(format(MPA_NOT_FOUND, film.getMpa().getId()));
            }
            film.setMpa(mpaDao.get(film.getMpa().getId()));
        }
        return films;
    }

    @Override
    public Collection<Film> getPopularFilms(int count) {
        log.debug("getPopularFilms({}).", count);
        List<Film> popularFilms = filmStorage.getAll().stream()
                .sorted(this::likeCompare)
                .limit(count)
                .collect(Collectors.toList());
        log.trace("Возвращены популярные фильмы: {}.", popularFilms);
        return popularFilms;
    }

    @Override
    public void addLike(long filmID, long userID) {
        log.debug("addLike({}, {}).", filmID, userID);
        if (!filmStorage.contains(filmID)) {
            log.warn("Не удалось добавить лайк: {}.", format(FILM_NOT_FOUND, filmID));
            throw new FilmNotFoundException(format(FILM_NOT_FOUND, filmID));
        }
        if (!userStorage.contains(userID)) {
            log.warn("Не удалось добавить лайк: {}.", format(USER_NOT_FOUND, userID));
            throw new UserNotFoundException(format(USER_NOT_FOUND, userID));
        }
        if (likeDao.contains(filmID, userID)) {
            log.warn("Не удалось добавить лайк: {}.", format(LIKE_ALREADY_EXISTS, filmID, userID));
            throw new LikeAlreadyExistsException(format(LIKE_ALREADY_EXISTS, filmID, userID));
        }
        likeDao.add(filmID, userID);
    }

    @Override
    public void deleteLike(long filmID, long userID) {
        log.debug("deleteLike({}, {}).", filmID, userID);
        if (!filmStorage.contains(filmID)) {
            log.warn("Не удалось удалить лайк: {}.", format(FILM_NOT_FOUND, filmID));
            throw new FilmNotFoundException(format(FILM_NOT_FOUND, filmID));
        }
        if (!userStorage.contains(userID)) {
            log.warn("Не удалось удалить лайк: {}.", format(USER_NOT_FOUND, userID));
            throw new UserNotFoundException(format(USER_NOT_FOUND, userID));
        }
        if (!likeDao.contains(filmID, userID)) {
            log.warn("Не удалось удалить лайк: {}.", format(LIKE_NOT_FOUND, filmID, userID));
            throw new LikeNotFoundException(format(LIKE_NOT_FOUND, filmID, userID));
        }
        likeDao.delete(filmID, userID);
    }

    /**
     * Метод сравнивает два фильма по количеству
     * лайков (в убывающем порядке).
     *
     * @param film      первый film для сравнения.
     * @param otherFilm второй film для сравнения.
     * @return Значение 0, если количество лайков
     * одинаковое; Значение меньше 0, если у первого
     * больше лайков, чем у второго; И значение
     * больше 0, если у второго больше лайков, чем
     * у первого.
     */
    private int likeCompare(Film film, Film otherFilm) {
        return Integer.compare(likeDao.count(otherFilm.getId()), likeDao.count(film.getId()));
    }
}