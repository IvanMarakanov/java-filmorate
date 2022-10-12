# java-filmorate

> Фильмов много — и с каждым годом становится всё больше.
> Чем их больше, тем больше разных оценок.
> Чем больше оценок, тем сложнее сделать выбор.
> Однако не время сдаваться!
> Перед вами бэкенд для сервиса, который будет
> работать с фильмами и оценками пользователей, а также
> возвращать топ фильмов, рекомендованных к просмотру.
> Теперь ни вам, ни вашим друзьям не придётся долго размышлять,
> что же посмотреть сегодня вечером.

## Валидация

Входные данные, которые приходят в запросе на добавление нового фильма 
или пользователя, должны соответствовать определённым критериям.

<details>
    <summary>Для фильмов:</summary>
        <ul>
            <li>название не может быть пустым</li>
            <li>максимальная длина описания — 200 символов</li>
            <li>дата релиза — не раньше 28 декабря 1895 года*</li>
            <li>продолжительность фильма должна быть положительной</li>
        </ul>
        *28 декабря 1895 года считается днём рождения кино.
</details>

<details>
    <summary>Для пользователей:</summary>
        <ul>
            <li>электронная почта не может быть пустой и должна содержать символ @</li>
            <li>логин не может быть пустым и содержать пробелы</li>
            <li>имя для отображения может быть пустым — в таком случае будет использован логин</li>
            <li>дата рождения не может быть в будущем</li>
        </ul>
</details>

## Схема БД

![](https://github.com/IvanMarakanov/java-filmorate/blob/main/src/main/resources/schema.png?raw=true)

## Примеры запросов

<details>
    <summary>Для фильмов:</summary>

* Получение списка всех фильмов:

```SQL
SELECT *
FROM films;
```

* Получение информации по фильму по его id:

```SQL
SELECT *
FROM films
WHERE films.film_id = ?;
```   

</details>

<details>
    <summary>Для пользователей:</summary>

* Получение списка всех пользователей:

```SQL
SELECT *
FROM users;
```

* Получение информации по пользователю по его id:

```SQL
SELECT *
FROM users
WHERE users.user_id = ?; -- id пользователя
```   

</details>
