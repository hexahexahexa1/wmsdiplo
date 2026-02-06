--
-- PostgreSQL database dump
--

\restrict Y1LioBOK0htDxEW2VgaFjphzPXx4L0XTfOS3IZaCei7Yd7dU9oC0VejYY0tLFbA

-- Dumped from database version 16.11 (Debian 16.11-1.pgdg13+1)
-- Dumped by pg_dump version 16.11 (Debian 16.11-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
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
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_logs (
    id bigint NOT NULL,
    entity_type character varying(64) NOT NULL,
    entity_id bigint NOT NULL,
    action character varying(32) NOT NULL,
    changed_by character varying(64),
    field_name character varying(64),
    old_value character varying(512),
    new_value character varying(512),
    "timestamp" timestamp without time zone NOT NULL,
    ip_address character varying(45),
    user_agent character varying(256)
);


--
-- Name: audit_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.audit_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: audit_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.audit_logs_id_seq OWNED BY public.audit_logs.id;


--
-- Name: discrepancies; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.discrepancies (
    id bigint NOT NULL,
    receipt_id bigint NOT NULL,
    line_id bigint,
    task_id bigint,
    pallet_id bigint,
    type character varying(50) NOT NULL,
    description text,
    qty_expected numeric(10,2),
    qty_actual numeric(10,2),
    resolved boolean DEFAULT false,
    resolved_by character varying(100),
    resolved_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: discrepancies_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.discrepancies_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: discrepancies_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.discrepancies_id_seq OWNED BY public.discrepancies.id;


--
-- Name: import_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.import_config (
    id bigint NOT NULL,
    config_key character varying(100) NOT NULL,
    config_value text NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: import_config_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.import_config_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: import_config_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.import_config_id_seq OWNED BY public.import_config.id;


--
-- Name: import_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.import_log (
    id bigint NOT NULL,
    file_name character varying(255) NOT NULL,
    status character varying(50) NOT NULL,
    message_id character varying(100),
    receipt_id bigint,
    error_message text,
    processed_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: import_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.import_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: import_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.import_log_id_seq OWNED BY public.import_log.id;


--
-- Name: locations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.locations (
    id bigint NOT NULL,
    code character varying(50) NOT NULL,
    zone_id bigint NOT NULL,
    location_type character varying(32) NOT NULL,
    status character varying(32) DEFAULT 'AVAILABLE'::character varying NOT NULL,
    aisle character varying(10),
    bay character varying(10),
    level character varying(10),
    x_coord integer,
    y_coord integer,
    z_coord integer,
    max_pallets integer DEFAULT 1,
    max_weight_kg numeric(10,2),
    max_width_cm numeric(8,2),
    max_height_cm numeric(8,2),
    max_depth_cm numeric(8,2),
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: locations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.locations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: locations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.locations_id_seq OWNED BY public.locations.id;


--
-- Name: packagings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.packagings (
    id bigint NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(255) NOT NULL,
    type character varying(50)
);


--
-- Name: packagings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.packagings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: packagings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.packagings_id_seq OWNED BY public.packagings.id;


--
-- Name: pallet_code_pool; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pallet_code_pool (
    id bigint NOT NULL,
    code character varying(50) NOT NULL,
    is_used boolean DEFAULT false,
    used_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: pallet_code_pool_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.pallet_code_pool_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pallet_code_pool_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.pallet_code_pool_id_seq OWNED BY public.pallet_code_pool.id;


--
-- Name: pallet_movements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pallet_movements (
    id bigint NOT NULL,
    pallet_id bigint NOT NULL,
    movement_type character varying(32) NOT NULL,
    from_location_id bigint,
    to_location_id bigint,
    quantity numeric(10,2),
    task_id bigint,
    moved_by character varying(128),
    moved_at timestamp without time zone
);


--
-- Name: pallet_movements_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.pallet_movements_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pallet_movements_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.pallet_movements_id_seq OWNED BY public.pallet_movements.id;


--
-- Name: pallets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pallets (
    id bigint NOT NULL,
    code character varying(50) NOT NULL,
    code_type character varying(20) DEFAULT 'INTERNAL'::character varying,
    sku_id bigint,
    receipt_id bigint,
    receipt_line_id bigint,
    location_id bigint,
    status character varying(32) DEFAULT 'DRAFT'::character varying NOT NULL,
    quantity numeric(10,2),
    uom character varying(10),
    weight_kg numeric(10,2),
    height_cm numeric(8,2),
    lot_number character varying(100),
    expiry_date date,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: pallets_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.pallets_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pallets_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.pallets_id_seq OWNED BY public.pallets.id;


--
-- Name: putaway_rules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.putaway_rules (
    id bigint NOT NULL,
    priority integer DEFAULT 100 NOT NULL,
    name character varying(128) NOT NULL,
    strategy_type character varying(32) NOT NULL,
    zone_id bigint,
    sku_category character varying(64),
    velocity_class character varying(1),
    params jsonb,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: putaway_rules_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.putaway_rules_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: putaway_rules_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.putaway_rules_id_seq OWNED BY public.putaway_rules.id;


--
-- Name: receipt_lines; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.receipt_lines (
    id bigint NOT NULL,
    receipt_id bigint NOT NULL,
    line_no integer NOT NULL,
    sku_id bigint NOT NULL,
    packaging_id bigint,
    uom character varying(10) NOT NULL,
    qty_expected numeric(10,2) NOT NULL,
    sscc_expected character varying(50),
    lot_number_expected character varying(100),
    expiry_date_expected date
);


--
-- Name: receipt_lines_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.receipt_lines_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: receipt_lines_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.receipt_lines_id_seq OWNED BY public.receipt_lines.id;


--
-- Name: receipts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.receipts (
    id bigint NOT NULL,
    doc_no character varying(50) NOT NULL,
    doc_date date NOT NULL,
    status character varying(32) DEFAULT 'DRAFT'::character varying NOT NULL,
    supplier character varying(255),
    message_id character varying(100),
    external_key character varying(100),
    cross_dock boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: receipts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.receipts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: receipts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.receipts_id_seq OWNED BY public.receipts.id;


--
-- Name: scans; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.scans (
    id bigint NOT NULL,
    task_id bigint NOT NULL,
    pallet_code character varying(50) NOT NULL,
    sscc character varying(50),
    barcode character varying(50),
    qty numeric(10,2) NOT NULL,
    device_id character varying(50),
    discrepancy boolean DEFAULT false,
    damage_flag boolean DEFAULT false,
    damage_type character varying(50),
    damage_description text,
    lot_number character varying(100),
    expiry_date date,
    scanned_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: scans_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.scans_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: scans_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.scans_id_seq OWNED BY public.scans.id;


--
-- Name: schema_version; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.schema_version (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: sku_storage_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sku_storage_config (
    id bigint NOT NULL,
    sku_id bigint NOT NULL,
    min_stock numeric(18,3),
    max_stock numeric(18,3),
    preferred_zone_id bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    velocity_class character varying(1) DEFAULT 'C'::character varying,
    hazmat_class character varying(16),
    temp_range character varying(32)
);


--
-- Name: sku_storage_config_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sku_storage_config_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sku_storage_config_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sku_storage_config_id_seq OWNED BY public.sku_storage_config.id;


--
-- Name: skus; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.skus (
    id bigint NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(255) NOT NULL,
    uom character varying(10) NOT NULL,
    pallet_capacity numeric(10,2)
);


--
-- Name: skus_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.skus_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: skus_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.skus_id_seq OWNED BY public.skus.id;


--
-- Name: status_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.status_history (
    id bigint NOT NULL,
    entity_type character varying(50) NOT NULL,
    entity_id bigint NOT NULL,
    old_status character varying(50),
    new_status character varying(50) NOT NULL,
    changed_by character varying(100),
    comment text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: status_history_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.status_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: status_history_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.status_history_id_seq OWNED BY public.status_history.id;


--
-- Name: tasks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tasks (
    id bigint NOT NULL,
    receipt_id bigint,
    line_id bigint,
    task_type character varying(32) NOT NULL,
    status character varying(32) DEFAULT 'NEW'::character varying NOT NULL,
    priority integer DEFAULT 0,
    qty_assigned numeric(10,2),
    qty_done numeric(10,2) DEFAULT 0,
    assignee character varying(100),
    assigned_by character varying(100),
    pallet_id bigint,
    source_location_id bigint,
    target_location_id bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    started_at timestamp without time zone,
    closed_at timestamp without time zone
);


--
-- Name: tasks_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tasks_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tasks_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tasks_id_seq OWNED BY public.tasks.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    username character varying(50) NOT NULL,
    password_hash character varying(255) NOT NULL,
    full_name character varying(255),
    role character varying(32) DEFAULT 'OPERATOR'::character varying NOT NULL,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    email character varying(255)
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: zones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.zones (
    id bigint NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    priority_rank integer DEFAULT 0,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: zones_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.zones_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: zones_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.zones_id_seq OWNED BY public.zones.id;


--
-- Name: audit_logs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs ALTER COLUMN id SET DEFAULT nextval('public.audit_logs_id_seq'::regclass);


--
-- Name: discrepancies id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discrepancies ALTER COLUMN id SET DEFAULT nextval('public.discrepancies_id_seq'::regclass);


--
-- Name: import_config id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.import_config ALTER COLUMN id SET DEFAULT nextval('public.import_config_id_seq'::regclass);


--
-- Name: import_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.import_log ALTER COLUMN id SET DEFAULT nextval('public.import_log_id_seq'::regclass);


--
-- Name: locations id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.locations ALTER COLUMN id SET DEFAULT nextval('public.locations_id_seq'::regclass);


--
-- Name: packagings id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.packagings ALTER COLUMN id SET DEFAULT nextval('public.packagings_id_seq'::regclass);


--
-- Name: pallet_code_pool id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pallet_code_pool ALTER COLUMN id SET DEFAULT nextval('public.pallet_code_pool_id_seq'::regclass);


--
-- Name: pallet_movements id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pallet_movements ALTER COLUMN id SET DEFAULT nextval('public.pallet_movements_id_seq'::regclass);


--
-- Name: pallets id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pallets ALTER COLUMN id SET DEFAULT nextval('public.pallets_id_seq'::regclass);


--
-- Name: putaway_rules id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.putaway_rules ALTER COLUMN id SET DEFAULT nextval('public.putaway_rules_id_seq'::regclass);


--
-- Name: receipt_lines id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.receipt_lines ALTER COLUMN id SET DEFAULT nextval('public.receipt_lines_id_seq'::regclass);


--
-- Name: receipts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.receipts ALTER COLUMN id SET DEFAULT nextval('public.receipts_id_seq'::regclass);


--
-- Name: scans id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scans ALTER COLUMN id SET DEFAULT nextval('public.scans_id_seq'::regclass);


--
-- Name: sku_storage_config id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sku_storage_config ALTER COLUMN id SET DEFAULT nextval('public.sku_storage_config_id_seq'::regclass);


--
-- Name: skus id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.skus ALTER COLUMN id SET DEFAULT nextval('public.skus_id_seq'::regclass);


--
-- Name: status_history id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.status_history ALTER COLUMN id SET DEFAULT nextval('public.status_history_id_seq'::regclass);


--
-- Name: tasks id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tasks ALTER COLUMN id SET DEFAULT nextval('public.tasks_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Name: zones id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.zones ALTER COLUMN id SET DEFAULT nextval('public.zones_id_seq'::regclass);


--
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.audit_logs (id, entity_type, entity_id, action, changed_by, field_name, old_value, new_value, "timestamp", ip_address, user_agent) FROM stdin;
\.


--
-- Data for Name: discrepancies; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.discrepancies (id, receipt_id, line_id, task_id, pallet_id, type, description, qty_expected, qty_actual, resolved, resolved_by, resolved_at, created_at) FROM stdin;
1	4	16	\N	\N	DAMAGE	\N	20.00	10.00	f	\N	\N	2026-01-12 02:15:31.712823
\.


--
-- Data for Name: import_config; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.import_config (id, config_key, config_value, updated_at) FROM stdin;
2	api_url	http://localhost:8080	2026-01-11 21:26:06.011515
1	import_folder	E:\\WMSDIPL\\import-data\\examples	2026-01-12 01:48:56.629893
\.


--
-- Data for Name: import_log; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.import_log (id, file_name, status, message_id, receipt_id, error_message, processed_at) FROM stdin;
\.


--
-- Data for Name: locations; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.locations (id, code, zone_id, location_type, status, aisle, bay, level, x_coord, y_coord, z_coord, max_pallets, max_weight_kg, max_width_cm, max_height_cm, max_depth_cm, is_active, created_at, updated_at) FROM stdin;
1	TEST	1	RECEIVING	AVAILABLE	1	1	1	\N	\N	\N	100	\N	\N	\N	\N	t	2026-01-12 02:13:05.483199	2026-01-12 02:13:05.483199
3	A-01-01	1	STORAGE	AVAILABLE	\N	\N	\N	\N	\N	\N	1	\N	\N	\N	\N	t	2026-01-11 23:38:23.777622	2026-01-11 23:38:23.777622
4	A-01-02	1	STORAGE	AVAILABLE	\N	\N	\N	\N	\N	\N	1	\N	\N	\N	\N	t	2026-01-11 23:38:23.777622	2026-01-11 23:38:23.777622
5	A-01-03	1	STORAGE	AVAILABLE	\N	\N	\N	\N	\N	\N	1	\N	\N	\N	\N	t	2026-01-11 23:38:23.777622	2026-01-11 23:38:23.777622
6	B-01-01	1	STORAGE	AVAILABLE	\N	\N	\N	\N	\N	\N	1	\N	\N	\N	\N	t	2026-01-11 23:38:23.777622	2026-01-11 23:38:23.777622
7	B-01-02	1	STORAGE	AVAILABLE	\N	\N	\N	\N	\N	\N	1	\N	\N	\N	\N	t	2026-01-11 23:38:23.777622	2026-01-11 23:38:23.777622
8	BRAK	1	QUARANTINE	AVAILABLE	C	02	02	\N	\N	\N	10	\N	\N	\N	\N	t	2026-01-12 02:43:14.472303	2026-01-12 02:43:14.472303
9	DAMAGE-01	1	DAMAGED	AVAILABLE	\N	\N	\N	\N	\N	\N	1	\N	\N	\N	\N	t	2026-01-11 23:44:35.344314	2026-01-11 23:44:35.344314
10	DAMAGE-02	1	DAMAGED	AVAILABLE	\N	\N	\N	\N	\N	\N	1	\N	\N	\N	\N	t	2026-01-11 23:44:35.344314	2026-01-11 23:44:35.344314
11	QUARANTINE-01	1	QUARANTINE	AVAILABLE	\N	\N	\N	\N	\N	\N	1	\N	\N	\N	\N	t	2026-01-11 23:44:35.344314	2026-01-11 23:44:35.344314
\.


--
-- Data for Name: packagings; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.packagings (id, code, name, type) FROM stdin;
\.


--
-- Data for Name: pallet_code_pool; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.pallet_code_pool (id, code, is_used, used_at, created_at) FROM stdin;
\.


--
-- Data for Name: pallet_movements; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.pallet_movements (id, pallet_id, movement_type, from_location_id, to_location_id, quantity, task_id, moved_by, moved_at) FROM stdin;
1	1	RECEIVE	\N	1	10.00	1	admin	2026-01-12 02:13:33.429904
2	2	RECEIVE	\N	1	5.00	2	admin	2026-01-12 02:14:38.422933
3	3	RECEIVE	\N	1	5.00	2	admin	2026-01-12 02:15:31.697808
4	1	PLACE	1	3	10.00	3	admin	2026-01-12 02:40:58.942747
5	2	PLACE	1	3	15.00	4	admin	2026-01-12 02:42:04.883742
6	1	PLACE	3	4	10.00	5	admin	2026-01-12 02:50:13.526096
7	2	PLACE	3	4	15.00	6	admin	2026-01-12 02:50:39.634002
8	3	PLACE	1	9	5.00	7	admin	2026-01-12 02:51:15.54527
\.


--
-- Data for Name: pallets; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.pallets (id, code, code_type, sku_id, receipt_id, receipt_line_id, location_id, status, quantity, uom, weight_kg, height_cm, lot_number, expiry_date, created_at, updated_at) FROM stdin;
1	PLT-00001	INTERNAL	40	4	15	4	PLACED	10.00	\N	\N	\N	\N	\N	2026-01-12 02:06:08.892345	2026-01-12 02:50:13.532337
2	PLT-00002	INTERNAL	41	4	16	4	PLACED	15.00	\N	\N	\N	\N	\N	2026-01-12 02:06:08.900924	2026-01-12 02:50:39.638266
3	PLT-00003	INTERNAL	41	4	16	9	PLACED	5.00	\N	\N	\N	\N	\N	2026-01-12 02:09:46.542004	2026-01-12 02:51:15.549842
\.


--
-- Data for Name: putaway_rules; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.putaway_rules (id, priority, name, strategy_type, zone_id, sku_category, velocity_class, params, is_active, created_at) FROM stdin;
1	100	Default fallback	CLOSEST	\N	\N	\N	\N	t	2026-01-11 22:32:09.141374
\.


--
-- Data for Name: receipt_lines; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.receipt_lines (id, receipt_id, line_no, sku_id, packaging_id, uom, qty_expected, sscc_expected, lot_number_expected, expiry_date_expected) FROM stdin;
3	3	1	28	\N	шт	200.00	00012345000000000001	\N	\N
4	3	2	29	\N	шт	150.00	00012345000000000002	\N	\N
5	3	3	30	\N	шт	100.00	00012345000000000003	\N	\N
6	3	4	31	\N	шт	80.00	00012345000000000004	\N	\N
7	3	5	32	\N	шт	120.00	00012345000000000005	\N	\N
8	3	6	33	\N	шт	200.00	00012345000000000006	\N	\N
9	3	7	34	\N	шт	150.00	00012345000000000007	\N	\N
10	3	8	35	\N	шт	50.00	00012345000000000008	\N	\N
11	3	9	36	\N	шт	300.00	00012345000000000009	\N	\N
12	3	10	37	\N	шт	250.00	00012345000000000010	\N	\N
13	3	11	38	\N	шт	180.00	00012345000000000011	\N	\N
14	3	12	39	\N	шт	240.00	00012345000000000012	\N	\N
15	4	1	40	\N	шт	10.00		\N	\N
16	4	2	41	\N	шт	20.00		\N	\N
17	5	1	42	\N	упак	500.00	00098765000000000001	\N	\N
18	5	2	43	\N	упак	300.00	00098765000000000002	\N	\N
19	5	3	44	\N	упак	200.00	00098765000000000003	\N	\N
20	5	4	45	\N	упак	600.00	00098765000000000004	\N	\N
21	5	5	46	\N	упак	250.00	00098765000000000005	\N	\N
22	6	1	47	\N	шт	100.00	00012345678901234567	\N	\N
23	6	2	48	\N	шт	50.00	00012345678901234568	\N	\N
24	6	3	49	\N	шт	75.00	00012345678901234569	\N	\N
25	7	1	50	\N	кг	45.50	00012345000000000020	\N	\N
26	7	2	51	\N	кг	30.75	00012345000000000021	\N	\N
27	7	3	52	\N	кг	120.25	00012345000000000022	\N	\N
28	7	4	53	\N	кг	80.50	00012345000000000023	\N	\N
29	7	5	54	\N	шт	50.00	00012345000000000024	\N	\N
\.


--
-- Data for Name: receipts; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.receipts (id, doc_no, doc_date, status, supplier, message_id, external_key, cross_dock, created_at, updated_at) FROM stdin;
3	RCP-2026-002	2026-01-12	DRAFT	Продуктовая База №1	MSG-2026-01-002	\N	f	2026-01-12 02:01:07.241702	2026-01-12 02:01:07.241702
5	RCP-PHARMA-001	2026-01-12	DRAFT	Фарма Дистрибьюция	MSG-PHARMA-001	\N	f	2026-01-12 02:01:07.53225	2026-01-12 02:01:07.53225
6	RCP-2026-001	2026-01-12	DRAFT	ООО Тестовый Поставщик	MSG-2026-01-001	\N	f	2026-01-12 02:01:07.677544	2026-01-12 02:01:07.677544
7	RCP-2026-004	2026-01-12	DRAFT	Весовые Товары ООО	MSG-2026-01-004	\N	f	2026-01-12 02:01:07.822642	2026-01-12 02:01:07.822642
4	RCP-MIN-001	2026-01-12	STOCKED		MSG-MIN-001	\N	f	2026-01-12 02:01:07.409852	2026-01-12 02:51:15.553428
\.


--
-- Data for Name: scans; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.scans (id, task_id, pallet_code, sscc, barcode, qty, device_id, discrepancy, damage_flag, damage_type, damage_description, lot_number, expiry_date, scanned_at) FROM stdin;
1	1	PLT-00001	\N	TEST-001	10.00	\N	f	f	\N	\N	\N	\N	2026-01-12 02:13:33.433254
2	2	PLT-00002	\N	TEST-002	5.00	\N	f	f	\N	\N	\N	\N	2026-01-12 02:14:38.423961
3	2	PLT-00003	\N	TEST-002	5.00	\N	t	t	PHYSICAL	разбили при доставании из грузовика	\N	\N	2026-01-12 02:15:31.709972
4	2	PLT-00002	\N	TEST-002	10.00	\N	f	f	\N	\N	\N	\N	2026-01-12 02:16:08.363167
7	5	PLT-00001	\N	TEST-001	10.00	\N	f	f	\N	\N	\N	\N	2026-01-12 02:50:13.528193
8	6	PLT-00002	\N	TEST-002	20.00	\N	f	f	\N	\N	\N	\N	2026-01-12 02:50:39.635654
9	7	PLT-00003	\N	TEST-002	5.00	\N	f	f	\N	\N	\N	\N	2026-01-12 02:51:15.54778
\.


--
-- Data for Name: schema_version; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.schema_version (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	999	Manual schema - migrations disabled	SQL	manual_schema.sql	0	manual	2026-01-11 21:26:06.012123	0	t
\.


--
-- Data for Name: sku_storage_config; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sku_storage_config (id, sku_id, min_stock, max_stock, preferred_zone_id, created_at, velocity_class, hazmat_class, temp_range) FROM stdin;
\.


--
-- Data for Name: skus; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.skus (id, code, name, uom, pallet_capacity) FROM stdin;
28	MILK-001	Молоко Простоквашино 3.2% 1л	шт	\N
29	MILK-002	Кефир 2.5% 1л	шт	\N
30	MILK-003	Творог 9% 200г	шт	\N
31	BREAD-001	Хлеб Бородинский нарезной 500г	шт	\N
32	BREAD-002	Батон нарезной 400г	шт	\N
33	BREAD-003	Булочка с маком 80г	шт	\N
34	OIL-001	Масло подсолнечное рафинированное 1л	шт	\N
35	OIL-002	Масло оливковое Extra Virgin 0.5л	шт	\N
36	CAN-001	Горошек зелёный консервированный 400г	шт	\N
37	CAN-002	Кукуруза консервированная 340г	шт	\N
38	DRINK-001	Сок яблочный 1л	шт	\N
39	DRINK-002	Вода минеральная газированная 1.5л	шт	\N
40	TEST-001	Тестовый товар 1	шт	\N
41	TEST-002	Тестовый товар 2	шт	\N
42	MED-001	Парацетамол таб. 500мг №20	упак	\N
43	MED-002	Аспирин таб. 500мг №10	упак	\N
44	MED-003	Но-шпа таб. 40мг №20	упак	\N
45	MED-004	Активированный уголь таб. №10	упак	\N
46	VIT-001	Витамин C 500мг №30	упак	\N
47	SKU-001	Молоко 3.2% 1л	шт	\N
48	SKU-002	Хлеб белый нарезной	шт	\N
49	SKU-003	Масло подсолнечное 1л	шт	\N
50	WEIGHT-001	Сыр Российский	кг	\N
51	WEIGHT-002	Колбаса Докторская	кг	\N
52	WEIGHT-003	Яблоки Голден	кг	\N
53	WEIGHT-004	Бананы	кг	\N
54	PIECE-001	Арбуз	шт	\N
\.


--
-- Data for Name: status_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.status_history (id, entity_type, entity_id, old_status, new_status, changed_by, comment, created_at) FROM stdin;
\.


--
-- Data for Name: tasks; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tasks (id, receipt_id, line_id, task_type, status, priority, qty_assigned, qty_done, assignee, assigned_by, pallet_id, source_location_id, target_location_id, created_at, started_at, closed_at) FROM stdin;
1	4	15	RECEIVING	COMPLETED	100	10.00	10.00	admin	system	\N	\N	\N	2026-01-12 02:05:10.144206	2026-01-12 02:11:25.467305	2026-01-12 02:13:35.802274
2	4	16	RECEIVING	COMPLETED	100	20.00	20.00	admin	system	\N	\N	\N	2026-01-12 02:05:10.148976	2026-01-12 02:14:21.44224	2026-01-12 02:25:09.067707
5	4	\N	PLACEMENT	COMPLETED	900	10.00	10.00	admin	system	1	3	4	2026-01-12 02:49:17.613398	2026-01-12 02:49:53.034481	2026-01-12 02:50:15.340004
6	4	\N	PLACEMENT	COMPLETED	900	15.00	20.00	admin	system	2	3	4	2026-01-12 02:49:17.646112	2026-01-12 02:50:20.580787	2026-01-12 02:50:41.184176
7	4	\N	PLACEMENT	COMPLETED	900	5.00	5.00	admin	system	3	1	9	2026-01-12 02:49:17.660515	2026-01-12 02:51:00.457258	2026-01-12 02:51:19.091085
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.users (id, username, password_hash, full_name, role, is_active, created_at, updated_at, email) FROM stdin;
2	admin	{bcrypt}$2a$10$ECjwig8kAjYxkf87VyldKeBw9CsU/BvIL7VK84LI7/W03jA65/JXi	System Administrator	ADMIN	t	2026-01-11 21:50:40.809432	2026-01-11 21:50:40.809432	\N
4	supervisor	{bcrypt}$2a$10$6YXoqyLP5c0e49JrD0Kc1OE4dxzt0yJ7F.pXcVW1tVg5cME2z9aFa	supervisor	SUPERVISOR	t	2026-01-12 01:30:01.206396	2026-01-12 01:30:01.206396	\N
\.


--
-- Data for Name: zones; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.zones (id, code, name, description, priority_rank, is_active, created_at, updated_at) FROM stdin;
1	TEST ZONE	ТЕСТОВАЯ ЗОНА	\N	100	t	2026-01-12 02:12:54.819071	2026-01-12 02:12:54.819071
\.


--
-- Name: audit_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.audit_logs_id_seq', 1, false);


--
-- Name: discrepancies_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.discrepancies_id_seq', 1, true);


--
-- Name: import_config_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.import_config_id_seq', 2, true);


--
-- Name: import_log_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.import_log_id_seq', 1, false);


--
-- Name: locations_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.locations_id_seq', 11, true);


--
-- Name: packagings_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.packagings_id_seq', 1, false);


--
-- Name: pallet_code_pool_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.pallet_code_pool_id_seq', 1, false);


--
-- Name: pallet_movements_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.pallet_movements_id_seq', 8, true);


--
-- Name: pallets_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.pallets_id_seq', 3, true);


--
-- Name: putaway_rules_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.putaway_rules_id_seq', 1, true);


--
-- Name: receipt_lines_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.receipt_lines_id_seq', 29, true);


--
-- Name: receipts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.receipts_id_seq', 7, true);


--
-- Name: scans_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.scans_id_seq', 9, true);


--
-- Name: sku_storage_config_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sku_storage_config_id_seq', 1, false);


--
-- Name: skus_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.skus_id_seq', 54, true);


--
-- Name: status_history_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.status_history_id_seq', 1, false);


--
-- Name: tasks_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tasks_id_seq', 7, true);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.users_id_seq', 4, true);


--
-- Name: zones_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.zones_id_seq', 1, true);


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: discrepancies discrepancies_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discrepancies
    ADD CONSTRAINT discrepancies_pkey PRIMARY KEY (id);


--
-- Name: import_config import_config_config_key_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.import_config
    ADD CONSTRAINT import_config_config_key_key UNIQUE (config_key);


--
-- Name: import_config import_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.import_config
    ADD CONSTRAINT import_config_pkey PRIMARY KEY (id);


--
-- Name: import_log import_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.import_log
    ADD CONSTRAINT import_log_pkey PRIMARY KEY (id);


--
-- Name: locations locations_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.locations
    ADD CONSTRAINT locations_code_key UNIQUE (code);


--
-- Name: locations locations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.locations
    ADD CONSTRAINT locations_pkey PRIMARY KEY (id);


--
-- Name: packagings packagings_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.packagings
    ADD CONSTRAINT packagings_code_key UNIQUE (code);


--
-- Name: packagings packagings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.packagings
    ADD CONSTRAINT packagings_pkey PRIMARY KEY (id);


--
-- Name: pallet_code_pool pallet_code_pool_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pallet_code_pool
    ADD CONSTRAINT pallet_code_pool_code_key UNIQUE (code);


--
-- Name: pallet_code_pool pallet_code_pool_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pallet_code_pool
    ADD CONSTRAINT pallet_code_pool_pkey PRIMARY KEY (id);


--
-- Name: pallet_movements pallet_movements_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pallet_movements
    ADD CONSTRAINT pallet_movements_pkey PRIMARY KEY (id);


--
-- Name: pallets pallets_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pallets
    ADD CONSTRAINT pallets_code_key UNIQUE (code);


--
-- Name: pallets pallets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pallets
    ADD CONSTRAINT pallets_pkey PRIMARY KEY (id);


--
-- Name: putaway_rules putaway_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.putaway_rules
    ADD CONSTRAINT putaway_rules_pkey PRIMARY KEY (id);


--
-- Name: receipt_lines receipt_lines_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.receipt_lines
    ADD CONSTRAINT receipt_lines_pkey PRIMARY KEY (id);


--
-- Name: receipt_lines receipt_lines_receipt_id_line_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.receipt_lines
    ADD CONSTRAINT receipt_lines_receipt_id_line_no_key UNIQUE (receipt_id, line_no);


--
-- Name: receipts receipts_doc_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.receipts
    ADD CONSTRAINT receipts_doc_no_key UNIQUE (doc_no);


--
-- Name: receipts receipts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.receipts
    ADD CONSTRAINT receipts_pkey PRIMARY KEY (id);


--
-- Name: scans scans_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scans
    ADD CONSTRAINT scans_pkey PRIMARY KEY (id);


--
-- Name: sku_storage_config sku_storage_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sku_storage_config
    ADD CONSTRAINT sku_storage_config_pkey PRIMARY KEY (id);


--
-- Name: skus skus_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.skus
    ADD CONSTRAINT skus_code_key UNIQUE (code);


--
-- Name: skus skus_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.skus
    ADD CONSTRAINT skus_pkey PRIMARY KEY (id);


--
-- Name: status_history status_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.status_history
    ADD CONSTRAINT status_history_pkey PRIMARY KEY (id);


--
-- Name: tasks tasks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tasks
    ADD CONSTRAINT tasks_pkey PRIMARY KEY (id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- Name: zones zones_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.zones
    ADD CONSTRAINT zones_code_key UNIQUE (code);


--
-- Name: zones zones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.zones
    ADD CONSTRAINT zones_pkey PRIMARY KEY (id);


--
-- Name: idx_audit_logs_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_entity ON public.audit_logs USING btree (entity_type, entity_id);


--
-- Name: idx_discrepancies_receipt; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_discrepancies_receipt ON public.discrepancies USING btree (receipt_id);


--
-- Name: idx_import_config_key; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_import_config_key ON public.import_config USING btree (config_key);


--
-- Name: idx_locations_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_locations_status ON public.locations USING btree (status);


--
-- Name: idx_locations_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_locations_type ON public.locations USING btree (location_type);


--
-- Name: idx_locations_zone; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_locations_zone ON public.locations USING btree (zone_id);


--
-- Name: idx_movements_pallet; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_movements_pallet ON public.pallet_movements USING btree (pallet_id);


--
-- Name: idx_movements_task; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_movements_task ON public.pallet_movements USING btree (task_id);


--
-- Name: idx_pallets_location; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pallets_location ON public.pallets USING btree (location_id);


--
-- Name: idx_pallets_receipt; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pallets_receipt ON public.pallets USING btree (receipt_id);


--
-- Name: idx_pallets_sku; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pallets_sku ON public.pallets USING btree (sku_id);


--
-- Name: idx_pallets_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pallets_status ON public.pallets USING btree (status);


--
-- Name: idx_putaway_rules_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_putaway_rules_active ON public.putaway_rules USING btree (is_active);


--
-- Name: idx_putaway_rules_priority; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_putaway_rules_priority ON public.putaway_rules USING btree (priority);


--
-- Name: idx_receipt_lines_receipt; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_receipt_lines_receipt ON public.receipt_lines USING btree (receipt_id);


--
-- Name: idx_receipt_lines_sku; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_receipt_lines_sku ON public.receipt_lines USING btree (sku_id);


--
-- Name: idx_receipts_doc_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_receipts_doc_date ON public.receipts USING btree (doc_date);


--
-- Name: idx_receipts_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_receipts_status ON public.receipts USING btree (status);


--
-- Name: idx_scans_pallet; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scans_pallet ON public.scans USING btree (pallet_code);


--
-- Name: idx_scans_task; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scans_task ON public.scans USING btree (task_id);


--
-- Name: idx_sku_storage_config_velocity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sku_storage_config_velocity ON public.sku_storage_config USING btree (velocity_class);


--
-- Name: idx_status_history_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_status_history_entity ON public.status_history USING btree (entity_type, entity_id);


--
-- Name: idx_tasks_assignee; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tasks_assignee ON public.tasks USING btree (assignee);


--
-- Name: idx_tasks_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tasks_created ON public.tasks USING btree (created_at);


--
-- Name: idx_tasks_receipt; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tasks_receipt ON public.tasks USING btree (receipt_id);


--
-- Name: idx_tasks_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tasks_status ON public.tasks USING btree (status);


--
-- Name: idx_tasks_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tasks_type ON public.tasks USING btree (task_type);


--
-- Name: discrepancies discrepancies_line_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discrepancies
    ADD CONSTRAINT discrepancies_line_id_fkey FOREIGN KEY (line_id) REFERENCES public.receipt_lines(id);


--
-- Name: discrepancies discrepancies_pallet_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discrepancies
    ADD CONSTRAINT discrepancies_pallet_id_fkey FOREIGN KEY (pallet_id) REFERENCES public.pallets(id);


--
-- Name: discrepancies discrepancies_receipt_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discrepancies
    ADD CONSTRAINT discrepancies_receipt_id_fkey FOREIGN KEY (receipt_id) REFERENCES public.receipts(id);


--
-- Name: discrepancies discrepancies_task_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discrepancies
    ADD CONSTRAINT discrepancies_task_id_fkey FOREIGN KEY (task_id) REFERENCES public.tasks(id);


--
-- Name: import_log import_log_receipt_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.import_log
    ADD CONSTRAINT import_log_receipt_id_fkey FOREIGN KEY (receipt_id) REFERENCES public.receipts(id);


--
-- Name: locations locations_zone_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.locations
    ADD CONSTRAINT locations_zone_id_fkey FOREIGN KEY (zone_id) REFERENCES public.zones(id);


--
-- Name: pallet_movements pallet_movements_from_location_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pallet_movements
    ADD CONSTRAINT pallet_movements_from_location_id_fkey FOREIGN KEY (from_location_id) REFERENCES public.locations(id);


--
-- Name: pallet_movements pallet_movements_pallet_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pallet_movements
    ADD CONSTRAINT pallet_movements_pallet_id_fkey FOREIGN KEY (pallet_id) REFERENCES public.pallets(id);


--
-- Name: pallet_movements pallet_movements_to_location_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pallet_movements
    ADD CONSTRAINT pallet_movements_to_location_id_fkey FOREIGN KEY (to_location_id) REFERENCES public.locations(id);


--
-- Name: pallets pallets_location_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pallets
    ADD CONSTRAINT pallets_location_id_fkey FOREIGN KEY (location_id) REFERENCES public.locations(id);


--
-- Name: pallets pallets_receipt_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pallets
    ADD CONSTRAINT pallets_receipt_id_fkey FOREIGN KEY (receipt_id) REFERENCES public.receipts(id);


--
-- Name: pallets pallets_receipt_line_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pallets
    ADD CONSTRAINT pallets_receipt_line_id_fkey FOREIGN KEY (receipt_line_id) REFERENCES public.receipt_lines(id);


--
-- Name: pallets pallets_sku_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pallets
    ADD CONSTRAINT pallets_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.skus(id);


--
-- Name: putaway_rules putaway_rules_zone_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.putaway_rules
    ADD CONSTRAINT putaway_rules_zone_id_fkey FOREIGN KEY (zone_id) REFERENCES public.zones(id);


--
-- Name: receipt_lines receipt_lines_packaging_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.receipt_lines
    ADD CONSTRAINT receipt_lines_packaging_id_fkey FOREIGN KEY (packaging_id) REFERENCES public.packagings(id);


--
-- Name: receipt_lines receipt_lines_receipt_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.receipt_lines
    ADD CONSTRAINT receipt_lines_receipt_id_fkey FOREIGN KEY (receipt_id) REFERENCES public.receipts(id) ON DELETE CASCADE;


--
-- Name: receipt_lines receipt_lines_sku_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.receipt_lines
    ADD CONSTRAINT receipt_lines_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.skus(id);


--
-- Name: scans scans_task_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scans
    ADD CONSTRAINT scans_task_id_fkey FOREIGN KEY (task_id) REFERENCES public.tasks(id);


--
-- Name: sku_storage_config sku_storage_config_preferred_zone_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sku_storage_config
    ADD CONSTRAINT sku_storage_config_preferred_zone_id_fkey FOREIGN KEY (preferred_zone_id) REFERENCES public.zones(id);


--
-- Name: sku_storage_config sku_storage_config_sku_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sku_storage_config
    ADD CONSTRAINT sku_storage_config_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES public.skus(id);


--
-- Name: tasks tasks_line_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tasks
    ADD CONSTRAINT tasks_line_id_fkey FOREIGN KEY (line_id) REFERENCES public.receipt_lines(id);


--
-- Name: tasks tasks_pallet_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tasks
    ADD CONSTRAINT tasks_pallet_id_fkey FOREIGN KEY (pallet_id) REFERENCES public.pallets(id);


--
-- Name: tasks tasks_receipt_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tasks
    ADD CONSTRAINT tasks_receipt_id_fkey FOREIGN KEY (receipt_id) REFERENCES public.receipts(id);


--
-- Name: tasks tasks_source_location_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tasks
    ADD CONSTRAINT tasks_source_location_id_fkey FOREIGN KEY (source_location_id) REFERENCES public.locations(id);


--
-- Name: tasks tasks_target_location_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tasks
    ADD CONSTRAINT tasks_target_location_id_fkey FOREIGN KEY (target_location_id) REFERENCES public.locations(id);


--
-- PostgreSQL database dump complete
--

\unrestrict Y1LioBOK0htDxEW2VgaFjphzPXx4L0XTfOS3IZaCei7Yd7dU9oC0VejYY0tLFbA

