--
-- PostgreSQL database dump
--

\restrict zuo6hjcmrP4yBYBe1R5KZgsekZpUA9SDWRC2LiQ6m9sptccE2RbXctgveq2V2fg

-- Dumped from database version 18.1
-- Dumped by pg_dump version 18.1

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: attachments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.attachments (
    id bigint NOT NULL,
    message_id bigint,
    file_url text NOT NULL,
    file_name character varying(255),
    file_size bigint,
    mime_type character varying(100),
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.attachments OWNER TO postgres;

--
-- Name: attachments_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.attachments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.attachments_id_seq OWNER TO postgres;

--
-- Name: attachments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.attachments_id_seq OWNED BY public.attachments.id;


--
-- Name: chat_members; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.chat_members (
    id bigint NOT NULL,
    chat_id bigint,
    user_id bigint,
    role character varying(20) DEFAULT 'member'::character varying,
    joined_at timestamp without time zone DEFAULT now(),
    muted_until timestamp without time zone,
    CONSTRAINT chat_members_role_check CHECK (((role)::text = ANY ((ARRAY['member'::character varying, 'admin'::character varying, 'owner'::character varying])::text[])))
);


ALTER TABLE public.chat_members OWNER TO postgres;

--
-- Name: chat_members_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.chat_members_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.chat_members_id_seq OWNER TO postgres;

--
-- Name: chat_members_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.chat_members_id_seq OWNED BY public.chat_members.id;


--
-- Name: chats; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.chats (
    id bigint NOT NULL,
    type character varying(20) DEFAULT 'private'::character varying NOT NULL,
    name character varying(100),
    avatar_url text,
    creator_id bigint,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    is_public boolean DEFAULT false NOT NULL,
    CONSTRAINT chats_type_check CHECK (((type)::text = ANY ((ARRAY['private'::character varying, 'group'::character varying])::text[])))
);


ALTER TABLE public.chats OWNER TO postgres;

--
-- Name: chats_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.chats_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.chats_id_seq OWNER TO postgres;

--
-- Name: chats_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.chats_id_seq OWNED BY public.chats.id;


--
-- Name: message_reads; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.message_reads (
    id bigint NOT NULL,
    chat_id bigint,
    user_id bigint,
    last_read_msg bigint,
    last_read_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.message_reads OWNER TO postgres;

--
-- Name: message_reads_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.message_reads_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.message_reads_id_seq OWNER TO postgres;

--
-- Name: message_reads_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.message_reads_id_seq OWNED BY public.message_reads.id;


--
-- Name: messages; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.messages (
    id bigint NOT NULL,
    chat_id bigint,
    sender_id bigint,
    reply_to_id bigint,
    type character varying(20) DEFAULT 'text'::character varying,
    text text,
    is_edited boolean DEFAULT false,
    is_deleted boolean DEFAULT false,
    send_date timestamp without time zone DEFAULT now(),
    edited_at timestamp without time zone,
    status character varying(20) DEFAULT 'SENT'::character varying,
    CONSTRAINT messages_status_check CHECK (((status)::text = ANY ((ARRAY['SENDING'::character varying, 'SENT'::character varying, 'DELIVERED'::character varying, 'READ'::character varying, 'NOT_SENDING'::character varying])::text[]))),
    CONSTRAINT messages_type_check CHECK (((type)::text = ANY ((ARRAY['text'::character varying, 'image'::character varying, 'images'::character varying, 'video'::character varying, 'audio'::character varying, 'file'::character varying])::text[])))
);


ALTER TABLE public.messages OWNER TO postgres;

--
-- Name: messages_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.messages_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.messages_id_seq OWNER TO postgres;

--
-- Name: messages_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.messages_id_seq OWNED BY public.messages.id;


--
-- Name: user_roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_roles (
    user_id bigint NOT NULL,
    role character varying(50) NOT NULL
);


ALTER TABLE public.user_roles OWNER TO postgres;

--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    username character varying(50) NOT NULL,
    email character varying(255) NOT NULL,
    email_verified boolean DEFAULT false,
    email_notifications boolean DEFAULT true,
    password character varying(255) NOT NULL,
    avatar_url text,
    status character varying(20) DEFAULT 'offline'::character varying,
    last_seen timestamp without time zone DEFAULT now(),
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    blocked boolean DEFAULT false NOT NULL,
    CONSTRAINT users_status_check CHECK (((status)::text = ANY ((ARRAY['online'::character varying, 'offline'::character varying])::text[])))
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.users_id_seq OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: attachments id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attachments ALTER COLUMN id SET DEFAULT nextval('public.attachments_id_seq'::regclass);


--
-- Name: chat_members id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat_members ALTER COLUMN id SET DEFAULT nextval('public.chat_members_id_seq'::regclass);


--
-- Name: chats id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chats ALTER COLUMN id SET DEFAULT nextval('public.chats_id_seq'::regclass);


--
-- Name: message_reads id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_reads ALTER COLUMN id SET DEFAULT nextval('public.message_reads_id_seq'::regclass);


--
-- Name: messages id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages ALTER COLUMN id SET DEFAULT nextval('public.messages_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Data for Name: attachments; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.attachments (id, message_id, file_url, file_name, file_size, mime_type, created_at) FROM stdin;
\.


--
-- Data for Name: chat_members; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.chat_members (id, chat_id, user_id, role, joined_at, muted_until) FROM stdin;
1	1	1	owner	2026-05-22 21:53:28.010774	\N
2	2	2	owner	2026-05-22 21:54:44.744065	\N
3	3	1	member	2026-05-22 21:56:14.206616	\N
4	3	2	member	2026-05-22 21:56:14.208006	\N
7	5	2	owner	2026-05-25 22:52:11.078932	\N
8	5	1	member	2026-05-25 22:52:11.078932	\N
9	6	2	owner	2026-05-25 22:52:18.562766	\N
10	6	1	member	2026-05-25 22:52:18.562766	\N
11	7	2	owner	2026-05-25 22:54:43.103146	\N
12	7	1	member	2026-05-25 22:54:43.103146	\N
13	8	3	owner	2026-05-25 22:57:18.51728	\N
14	9	4	owner	2026-05-25 22:58:35.162676	\N
15	10	5	owner	2026-05-25 23:00:51.481717	\N
16	11	5	member	2026-05-25 23:02:40.74703	\N
17	11	2	member	2026-05-25 23:02:40.747982	\N
18	12	5	member	2026-05-25 23:02:45.705015	\N
19	12	1	member	2026-05-25 23:02:45.706085	\N
20	13	5	owner	2026-05-25 23:08:35.506857	\N
21	13	1	member	2026-05-25 23:08:35.506857	\N
22	13	2	member	2026-05-25 23:08:35.506857	\N
23	13	3	member	2026-05-25 23:08:35.506857	\N
24	13	4	member	2026-05-25 23:08:35.506857	\N
\.


--
-- Data for Name: chats; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.chats (id, type, name, avatar_url, creator_id, created_at, updated_at, is_public) FROM stdin;
1	private	Заметки	\N	1	2026-05-22 21:53:28.010774	2026-05-22 21:53:28.010774	f
2	private	Заметки	\N	2	2026-05-22 21:54:44.744065	2026-05-22 21:54:44.744065	f
3	private	\N	\N	1	2026-05-22 21:56:14.204151	2026-05-22 21:56:14.204151	f
5	group	321	\N	2	2026-05-25 22:52:11.078932	2026-05-25 22:52:11.078932	f
6	group	312	\N	2	2026-05-25 22:52:18.562766	2026-05-25 22:52:18.562766	f
7	group	321	\N	2	2026-05-25 22:54:43.103146	2026-05-25 22:54:43.103146	f
8	private	Заметки	\N	3	2026-05-25 22:57:18.51728	2026-05-25 22:57:18.51728	f
9	private	Заметки	\N	4	2026-05-25 22:58:35.162676	2026-05-25 22:58:35.162676	f
10	private	Заметки	\N	5	2026-05-25 23:00:51.481717	2026-05-25 23:00:51.481717	f
11	private	\N	\N	5	2026-05-25 23:02:40.74552	2026-05-25 23:02:40.74552	f
12	private	\N	\N	5	2026-05-25 23:02:45.703509	2026-05-25 23:02:45.703509	f
13	group	шашлыки	\N	5	2026-05-25 23:08:35.506857	2026-05-25 23:08:35.506857	f
\.


--
-- Data for Name: message_reads; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.message_reads (id, chat_id, user_id, last_read_msg, last_read_at) FROM stdin;
11	1	1	\N	2026-05-25 22:18:45.226995
16	6	2	\N	2026-05-25 22:52:23.561156
18	7	2	\N	2026-05-25 22:54:44.261061
5	3	2	8	2026-05-25 22:55:28.184312
22	11	5	\N	2026-05-25 23:02:40.764585
23	12	5	\N	2026-05-25 23:02:45.718998
25	13	5	\N	2026-05-25 23:08:36.789493
29	5	1	\N	2026-05-25 23:49:48.232065
24	12	1	38	2026-05-28 15:51:20.532183
27	13	1	39	2026-05-28 15:51:21.603135
28	7	1	\N	2026-05-28 15:51:23.180403
1	3	1	8	2026-05-28 15:51:23.815461
\.


--
-- Data for Name: messages; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.messages (id, chat_id, sender_id, reply_to_id, type, text, is_edited, is_deleted, send_date, edited_at, status) FROM stdin;
6	1	1	\N	text	куку	f	f	2026-05-25 22:18:49.623042	\N	SENT
7	1	1	\N	text	планы на след. неделю	f	f	2026-05-25 22:18:57.602266	\N	SENT
1	3	1	\N	text	бурмалда	f	f	2026-05-22 21:56:21.513147	\N	READ
8	3	1	\N	text	321	f	f	2026-05-25 22:55:01.862304	\N	READ
9	12	5	\N	text	321312	f	f	2026-05-25 23:02:47.880643	\N	SENT
10	12	5	\N	text	312	f	f	2026-05-25 23:02:48.048537	\N	SENT
11	12	5	\N	text	312	f	f	2026-05-25 23:02:48.192236	\N	SENT
12	12	5	\N	text	312	f	f	2026-05-25 23:02:48.324481	\N	SENT
13	12	5	\N	text	3	f	f	2026-05-25 23:02:48.458997	\N	SENT
14	12	5	\N	text	123	f	f	2026-05-25 23:02:48.591784	\N	SENT
15	12	5	\N	text	123	f	f	2026-05-25 23:02:48.736228	\N	SENT
16	12	5	\N	text	12	f	f	2026-05-25 23:02:48.876382	\N	SENT
17	12	5	\N	text	312	f	f	2026-05-25 23:02:49.013171	\N	SENT
18	12	5	\N	text	312	f	f	2026-05-25 23:02:49.150773	\N	SENT
19	12	5	\N	text	312	f	f	2026-05-25 23:02:49.2937	\N	SENT
24	12	5	\N	text	2312	f	f	2026-05-25 23:02:49.998161	\N	READ
25	12	5	\N	text	312	f	f	2026-05-25 23:02:50.160179	\N	READ
27	12	5	\N	text	31	f	f	2026-05-25 23:02:50.448067	\N	READ
26	12	5	\N	text	31	f	f	2026-05-25 23:02:50.293697	\N	READ
23	12	5	\N	text	231	f	f	2026-05-25 23:02:49.856066	\N	READ
22	12	5	\N	text	31	f	f	2026-05-25 23:02:49.711259	\N	READ
21	12	5	\N	text	312	f	f	2026-05-25 23:02:49.578775	\N	READ
20	12	5	\N	text	312	f	f	2026-05-25 23:02:49.442189	\N	READ
39	13	1	\N	text	привет барни	f	f	2026-05-26 18:20:25.678726	\N	SENT
32	12	5	\N	text	312	f	f	2026-05-25 23:02:51.179163	\N	READ
28	12	5	\N	text	312	f	f	2026-05-25 23:02:50.600493	\N	READ
31	12	5	\N	text	312	f	f	2026-05-25 23:02:51.044596	\N	READ
37	12	5	\N	text	312	f	f	2026-05-25 23:02:51.918246	\N	READ
30	12	5	\N	text	312	f	f	2026-05-25 23:02:50.881333	\N	READ
35	12	5	\N	text	312	f	f	2026-05-25 23:02:51.629536	\N	READ
33	12	5	\N	text	312	f	f	2026-05-25 23:02:51.340276	\N	READ
36	12	5	\N	text	312	f	f	2026-05-25 23:02:51.777043	\N	READ
34	12	5	\N	text	312	f	f	2026-05-25 23:02:51.485847	\N	READ
29	12	5	\N	text	312	f	f	2026-05-25 23:02:50.742853	\N	READ
38	12	5	\N	text	33123	f	f	2026-05-25 23:02:52.062383	\N	READ
\.


--
-- Data for Name: user_roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_roles (user_id, role) FROM stdin;
2	ROLE_USER
3	ROLE_USER
4	ROLE_USER
5	ROLE_USER
1	ROLE_ADMIN
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, username, email, email_verified, email_notifications, password, avatar_url, status, last_seen, created_at, updated_at, blocked) FROM stdin;
2	burmaldatik	burmalda@mail.ru	t	t	$2a$10$X0BQBV07fNpOMY/r8qAhmeoKECaMxHT0RS2TlU8babPr7CL83KDwq	/uploads/avatars/4e534c5e-7f77-432d-97a4-5b882271e2a8.png	offline	2026-05-25 22:56:45.907496	2026-05-22 21:54:44.744065	2026-05-25 22:56:45.907496	f
5	test3	test@mail.com	t	t	$2a$10$9sNli82NBh/RHSXT5kpT0eZFGCqv3yhgW4M39H4LMRskXWfvQMhWK	\N	offline	2026-05-25 23:12:39.868028	2026-05-25 23:00:51.481717	2026-05-25 23:12:39.868028	f
3	test	222@mail.com	t	t	$2a$10$.ZMJ45VO3AhP5Aez5p3OMeQLa1eCZ3z9PqpU35RPhW8oug/AmEsUy	\N	offline	2026-05-25 22:57:18.587278	2026-05-25 22:57:18.51728	2026-05-25 22:57:46.734702	f
4	test2	2222@mail.ru	f	t	$2a$10$dYasJKXg2UZMLZ2ZgZN3hu3zMta5V2/kX6rAg3Rtn79ABXzmBXpaO	\N	offline	2026-05-25 22:58:35.222059	2026-05-25 22:58:35.162676	2026-05-25 22:58:35.162676	f
1	1v1ce	bos.belousov@mail.ru	t	f	$2a$10$N3jnOFTgBH2fJOjiim7kVepwWeH2hBkHSw.pM79GlJgUuff2fJ.na	/uploads/avatars/e30495d3-12ab-4555-b0ba-5a780cd5731b.jpg	online	2026-05-28 15:51:35.745593	2026-05-22 21:53:28.010774	2026-05-28 15:51:35.745593	f
\.


--
-- Name: attachments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.attachments_id_seq', 3, true);


--
-- Name: chat_members_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.chat_members_id_seq', 29, true);


--
-- Name: chats_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.chats_id_seq', 14, true);


--
-- Name: message_reads_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.message_reads_id_seq', 45, true);


--
-- Name: messages_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.messages_id_seq', 39, true);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.users_id_seq', 5, true);


--
-- Name: attachments attachments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attachments
    ADD CONSTRAINT attachments_pkey PRIMARY KEY (id);


--
-- Name: chat_members chat_members_chat_id_user_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat_members
    ADD CONSTRAINT chat_members_chat_id_user_id_key UNIQUE (chat_id, user_id);


--
-- Name: chat_members chat_members_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat_members
    ADD CONSTRAINT chat_members_pkey PRIMARY KEY (id);


--
-- Name: chats chats_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chats
    ADD CONSTRAINT chats_pkey PRIMARY KEY (id);


--
-- Name: message_reads message_reads_chat_id_user_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_reads
    ADD CONSTRAINT message_reads_chat_id_user_id_key UNIQUE (chat_id, user_id);


--
-- Name: message_reads message_reads_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_reads
    ADD CONSTRAINT message_reads_pkey PRIMARY KEY (id);


--
-- Name: messages messages_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_pkey PRIMARY KEY (id);


--
-- Name: user_roles pk_user_roles; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- Name: idx_attachments_message; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_attachments_message ON public.attachments USING btree (message_id);


--
-- Name: idx_chat_members_chat; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_chat_members_chat ON public.chat_members USING btree (chat_id);


--
-- Name: idx_chat_members_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_chat_members_user ON public.chat_members USING btree (user_id);


--
-- Name: idx_chats_creator_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_chats_creator_id ON public.chats USING btree (creator_id);


--
-- Name: idx_chats_is_public; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_chats_is_public ON public.chats USING btree (is_public);


--
-- Name: idx_chats_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_chats_type ON public.chats USING btree (type);


--
-- Name: idx_messages_chat_created; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_messages_chat_created ON public.messages USING btree (chat_id, send_date DESC);


--
-- Name: idx_messages_reply_to; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_messages_reply_to ON public.messages USING btree (reply_to_id);


--
-- Name: idx_messages_sender; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_messages_sender ON public.messages USING btree (sender_id);


--
-- Name: idx_reads_chat_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_reads_chat_user ON public.message_reads USING btree (chat_id, user_id);


--
-- Name: idx_user_roles_role; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_roles_role ON public.user_roles USING btree (role);


--
-- Name: idx_user_roles_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_roles_user ON public.user_roles USING btree (user_id);


--
-- Name: idx_users_blocked; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_blocked ON public.users USING btree (blocked);


--
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_email ON public.users USING btree (email);


--
-- Name: idx_users_username; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_username ON public.users USING btree (username);


--
-- Name: attachments attachments_message_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attachments
    ADD CONSTRAINT attachments_message_id_fkey FOREIGN KEY (message_id) REFERENCES public.messages(id) ON DELETE CASCADE;


--
-- Name: chat_members chat_members_chat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat_members
    ADD CONSTRAINT chat_members_chat_id_fkey FOREIGN KEY (chat_id) REFERENCES public.chats(id) ON DELETE CASCADE;


--
-- Name: chat_members chat_members_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat_members
    ADD CONSTRAINT chat_members_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: chats chats_creator_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chats
    ADD CONSTRAINT chats_creator_id_fkey FOREIGN KEY (creator_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: message_reads message_reads_chat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_reads
    ADD CONSTRAINT message_reads_chat_id_fkey FOREIGN KEY (chat_id) REFERENCES public.chats(id) ON DELETE CASCADE;


--
-- Name: message_reads message_reads_last_read_msg_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_reads
    ADD CONSTRAINT message_reads_last_read_msg_fkey FOREIGN KEY (last_read_msg) REFERENCES public.messages(id) ON DELETE SET NULL;


--
-- Name: message_reads message_reads_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_reads
    ADD CONSTRAINT message_reads_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: messages messages_chat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_chat_id_fkey FOREIGN KEY (chat_id) REFERENCES public.chats(id) ON DELETE CASCADE;


--
-- Name: messages messages_reply_to_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_reply_to_id_fkey FOREIGN KEY (reply_to_id) REFERENCES public.messages(id) ON DELETE SET NULL;


--
-- Name: messages messages_sender_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: user_roles user_roles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict zuo6hjcmrP4yBYBe1R5KZgsekZpUA9SDWRC2LiQ6m9sptccE2RbXctgveq2V2fg

