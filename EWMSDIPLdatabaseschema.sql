--
-- PostgreSQL database dump
--

\restrict eResoC1wWtNoaq3UfMLG3b6jlC3z0M0FRZLfW1Z5wSMFFK9g2iS5AZYDcGnhl59

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
    "timestamp" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ip_address character varying(45),
    user_agent character varying(256)
);


--
-- Name: TABLE audit_logs; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.audit_logs IS 'Audit trail for all entity changes in the system';


--
-- Name: COLUMN audit_logs.entity_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.audit_logs.entity_type IS 'Type of entity (e.g., Receipt, Pallet, Task)';


--
-- Name: COLUMN audit_logs.entity_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.audit_logs.entity_id IS 'ID of the entity that was changed';


--
-- Name: COLUMN audit_logs.action; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.audit_logs.action IS 'Action performed (CREATE, UPDATE, DELETE, etc.)';


--
-- Name: COLUMN audit_logs.changed_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.audit_logs.changed_by IS 'Username of the person who made the change';


--
-- Name: COLUMN audit_logs.field_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.audit_logs.field_name IS 'Name of the field that was changed (for UPDATE actions)';


--
-- Name: COLUMN audit_logs.old_value; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.audit_logs.old_value IS 'Previous value of the field';


--
-- Name: COLUMN audit_logs.new_value; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.audit_logs.new_value IS 'New value of the field';


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
    type character varying(64),
    qty_expected numeric(18,3),
    qty_actual numeric(18,3),
    comment character varying(512),
    resolved boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT now() NOT NULL
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
    checksum character varying(128),
    message_id character varying(128),
    status character varying(32) NOT NULL,
    error_message text,
    processed_at timestamp without time zone DEFAULT now() NOT NULL
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
    zone_id bigint NOT NULL,
    code character varying(32) NOT NULL,
    aisle character varying(8),
    bay character varying(8),
    level character varying(8),
    x_coord numeric(10,2),
    y_coord numeric(10,2),
    z_coord numeric(10,2),
    max_weight_kg numeric(10,2),
    max_height_cm numeric(10,2),
    max_width_cm numeric(10,2),
    max_depth_cm numeric(10,2),
    max_pallets integer DEFAULT 1,
    status character varying(32) DEFAULT 'AVAILABLE'::character varying,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    location_type character varying(32) DEFAULT 'STORAGE'::character varying
);


--
-- Name: COLUMN locations.location_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.locations.location_type IS 'Location type: RECEIVING, STORAGE, SHIPPING, CROSS_DOCK, DAMAGED';


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
    code character varying(64) NOT NULL,
    name character varying(255) NOT NULL,
    capacity numeric(18,2),
    uom character varying(32)
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
    code character varying(64) NOT NULL,
    code_type character varying(16) DEFAULT 'INTERNAL'::character varying,
    is_used boolean DEFAULT false,
    generated_at timestamp without time zone DEFAULT now(),
    used_at timestamp without time zone
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
    from_location_id bigint,
    to_location_id bigint,
    movement_type character varying(32),
    task_id bigint,
    moved_by character varying(128),
    moved_at timestamp without time zone DEFAULT now(),
    quantity numeric(10,2)
);


--
-- Name: COLUMN pallet_movements.quantity; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pallet_movements.quantity IS 'Quantity involved in this movement (for partial picks/adjustments). NULL means full pallet moved.';


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
    code character varying(64) NOT NULL,
    code_type character varying(16) DEFAULT 'INTERNAL'::character varying,
    status character varying(32) DEFAULT 'EMPTY'::character varying NOT NULL,
    location_id bigint,
    sku_id bigint,
    lot_number character varying(64),
    expiry_date date,
    quantity numeric(18,3),
    uom character varying(32),
    receipt_id bigint,
    receipt_line_id bigint,
    weight_kg numeric(10,2),
    height_cm numeric(10,2),
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now()
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
    sku_id bigint,
    packaging_id bigint,
    uom character varying(32),
    qty_expected numeric(18,3) NOT NULL,
    sscc_expected character varying(64),
    line_no integer,
    lot_number_expected character varying(128),
    expiry_date_expected date
);


--
-- Name: COLUMN receipt_lines.lot_number_expected; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.receipt_lines.lot_number_expected IS 'Expected lot/batch number from supplier document';


--
-- Name: COLUMN receipt_lines.expiry_date_expected; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.receipt_lines.expiry_date_expected IS 'Expected expiry date from supplier document';


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
    external_key character varying(128),
    doc_no character varying(64) NOT NULL,
    doc_date date,
    supplier character varying(255),
    status character varying(32) NOT NULL,
    message_id character varying(128),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    cross_dock boolean DEFAULT false
);


--
-- Name: COLUMN receipts.cross_dock; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.receipts.cross_dock IS 'True if receipt should bypass storage (cross-docking workflow)';


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
    task_id bigint,
    sscc character varying(64),
    barcode character varying(128),
    qty numeric(18,3),
    device_id character varying(128),
    discrepancy boolean DEFAULT false,
    scanned_at timestamp without time zone DEFAULT now() NOT NULL,
    pallet_code character varying(64),
    damage_flag boolean DEFAULT false,
    damage_type character varying(64),
    damage_description character varying(512),
    lot_number character varying(128),
    expiry_date date
);


--
-- Name: COLUMN scans.pallet_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scans.pallet_code IS 'Pallet code scanned during receiving. Links scan to specific pallet.';


--
-- Name: COLUMN scans.damage_flag; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scans.damage_flag IS 'True if goods are damaged during receiving';


--
-- Name: COLUMN scans.damage_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scans.damage_type IS 'Damage type: PHYSICAL_DAMAGE, WATER_DAMAGE, EXPIRED, TEMPERATURE_ABUSE, CONTAMINATION, OTHER';


--
-- Name: COLUMN scans.damage_description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scans.damage_description IS 'Free text description of damage';


--
-- Name: COLUMN scans.lot_number; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scans.lot_number IS 'Scanned lot/batch number';


--
-- Name: COLUMN scans.expiry_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scans.expiry_date IS 'Scanned expiry date';


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
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: sku_storage_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sku_storage_config (
    id bigint NOT NULL,
    sku_id bigint NOT NULL,
    velocity_class character varying(1) DEFAULT 'C'::character varying,
    preferred_zone_id bigint,
    hazmat_class character varying(16),
    temp_range character varying(32),
    min_stock numeric(18,3) DEFAULT 0,
    max_stock numeric(18,3),
    created_at timestamp without time zone DEFAULT now()
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
    code character varying(64) NOT NULL,
    name character varying(255) NOT NULL,
    uom character varying(32) NOT NULL,
    pallet_capacity numeric(10,2)
);


--
-- Name: COLUMN skus.pallet_capacity; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.skus.pallet_capacity IS 'Standard pallet quantity for auto-splitting large receipt lines into multiple tasks';


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
    entity_id bigint NOT NULL,
    entity_type character varying(64) NOT NULL,
    status_from character varying(32),
    status_to character varying(32) NOT NULL,
    changed_by character varying(128),
    changed_at timestamp without time zone DEFAULT now() NOT NULL
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
    receipt_id bigint NOT NULL,
    line_id bigint,
    assignee character varying(128),
    status character varying(32) NOT NULL,
    qty_assigned numeric(18,3),
    qty_done numeric(18,3),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    closed_at timestamp without time zone,
    task_type character varying(32) DEFAULT 'RECEIVING'::character varying,
    pallet_id bigint,
    source_location_id bigint,
    target_location_id bigint,
    assigned_by character varying(128),
    started_at timestamp without time zone,
    priority integer DEFAULT 100
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
    username character varying(64) NOT NULL,
    password_hash character varying(256) NOT NULL,
    full_name character varying(255),
    email character varying(255),
    role character varying(32) DEFAULT 'OPERATOR'::character varying NOT NULL,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now()
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
    code character varying(32) NOT NULL,
    name character varying(128) NOT NULL,
    priority_rank integer DEFAULT 100,
    description character varying(512),
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now()
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
-- Name: import_log import_log_message_id_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.import_log
    ADD CONSTRAINT import_log_message_id_unique UNIQUE (message_id);


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
-- Name: receipts receipts_doc_supplier; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.receipts
    ADD CONSTRAINT receipts_doc_supplier UNIQUE (doc_no, supplier);


--
-- Name: receipts receipts_message_id_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.receipts
    ADD CONSTRAINT receipts_message_id_unique UNIQUE (message_id);


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
-- Name: schema_version schema_version_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schema_version
    ADD CONSTRAINT schema_version_pk PRIMARY KEY (installed_rank);


--
-- Name: sku_storage_config sku_storage_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sku_storage_config
    ADD CONSTRAINT sku_storage_config_pkey PRIMARY KEY (id);


--
-- Name: sku_storage_config sku_storage_config_sku_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sku_storage_config
    ADD CONSTRAINT sku_storage_config_sku_id_key UNIQUE (sku_id);


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
-- Name: idx_audit_logs_changed_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_changed_by ON public.audit_logs USING btree (changed_by);


--
-- Name: idx_audit_logs_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_entity ON public.audit_logs USING btree (entity_type, entity_id);


--
-- Name: idx_audit_logs_entity_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_entity_type ON public.audit_logs USING btree (entity_type);


--
-- Name: idx_audit_logs_timestamp; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_timestamp ON public.audit_logs USING btree ("timestamp");


--
-- Name: idx_discrepancies_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_discrepancies_type ON public.discrepancies USING btree (type);


--
-- Name: idx_import_config_key; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_import_config_key ON public.import_config USING btree (config_key);


--
-- Name: idx_locations_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_locations_code ON public.locations USING btree (code);


--
-- Name: idx_locations_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_locations_status ON public.locations USING btree (status);


--
-- Name: idx_locations_zone; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_locations_zone ON public.locations USING btree (zone_id);


--
-- Name: idx_movement_moved_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_movement_moved_at ON public.pallet_movements USING btree (moved_at);


--
-- Name: idx_movement_pallet_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_movement_pallet_id ON public.pallet_movements USING btree (pallet_id);


--
-- Name: idx_movement_pallet_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_movement_pallet_time ON public.pallet_movements USING btree (pallet_id, moved_at DESC);


--
-- Name: idx_pallet_location_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pallet_location_id ON public.pallets USING btree (location_id);


--
-- Name: idx_pallet_movements_pallet; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pallet_movements_pallet ON public.pallet_movements USING btree (pallet_id);


--
-- Name: idx_pallet_receipt_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pallet_receipt_id ON public.pallets USING btree (receipt_id);


--
-- Name: idx_pallet_sku_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pallet_sku_id ON public.pallets USING btree (sku_id);


--
-- Name: idx_pallet_sku_location; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pallet_sku_location ON public.pallets USING btree (sku_id, location_id) WHERE (quantity > (0)::numeric);


--
-- Name: idx_pallet_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pallet_status ON public.pallets USING btree (status);


--
-- Name: idx_pallets_location; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pallets_location ON public.pallets USING btree (location_id);


--
-- Name: idx_pallets_lot_number; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pallets_lot_number ON public.pallets USING btree (lot_number);


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
-- Name: idx_receipts_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_receipts_status ON public.receipts USING btree (status);


--
-- Name: idx_receipts_status_dates; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_receipts_status_dates ON public.receipts USING btree (status, created_at);


--
-- Name: idx_scans_damage_flag; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scans_damage_flag ON public.scans USING btree (damage_flag) WHERE (damage_flag = true);


--
-- Name: idx_scans_lot_number; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scans_lot_number ON public.scans USING btree (lot_number);


--
-- Name: idx_scans_pallet_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scans_pallet_code ON public.scans USING btree (pallet_code);


--
-- Name: idx_sku_storage_config_velocity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sku_storage_config_velocity ON public.sku_storage_config USING btree (velocity_class);


--
-- Name: idx_tasks_assignee; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tasks_assignee ON public.tasks USING btree (assignee);


--
-- Name: idx_tasks_pallet; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tasks_pallet ON public.tasks USING btree (pallet_id);


--
-- Name: idx_tasks_priority; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tasks_priority ON public.tasks USING btree (priority DESC, created_at);


--
-- Name: idx_tasks_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tasks_status ON public.tasks USING btree (status);


--
-- Name: idx_tasks_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tasks_type ON public.tasks USING btree (task_type);


--
-- Name: schema_version_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX schema_version_s_idx ON public.schema_version USING btree (success);


--
-- Name: discrepancies discrepancies_line_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discrepancies
    ADD CONSTRAINT discrepancies_line_id_fkey FOREIGN KEY (line_id) REFERENCES public.receipt_lines(id) ON DELETE SET NULL;


--
-- Name: discrepancies discrepancies_receipt_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discrepancies
    ADD CONSTRAINT discrepancies_receipt_id_fkey FOREIGN KEY (receipt_id) REFERENCES public.receipts(id) ON DELETE CASCADE;


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
-- Name: pallet_movements pallet_movements_task_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pallet_movements
    ADD CONSTRAINT pallet_movements_task_id_fkey FOREIGN KEY (task_id) REFERENCES public.tasks(id);


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
    ADD CONSTRAINT scans_task_id_fkey FOREIGN KEY (task_id) REFERENCES public.tasks(id) ON DELETE CASCADE;


--
-- Name: sku_storage_config sku_storage_config_preferred_zone_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sku_storage_config
    ADD CONSTRAINT sku_storage_config_preferred_zone_id_fkey FOREIGN KEY (preferred_zone_id) REFERENCES public.zones(id);


--
-- Name: tasks tasks_line_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tasks
    ADD CONSTRAINT tasks_line_id_fkey FOREIGN KEY (line_id) REFERENCES public.receipt_lines(id) ON DELETE SET NULL;


--
-- Name: tasks tasks_pallet_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tasks
    ADD CONSTRAINT tasks_pallet_id_fkey FOREIGN KEY (pallet_id) REFERENCES public.pallets(id);


--
-- Name: tasks tasks_receipt_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tasks
    ADD CONSTRAINT tasks_receipt_id_fkey FOREIGN KEY (receipt_id) REFERENCES public.receipts(id) ON DELETE CASCADE;


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

\unrestrict eResoC1wWtNoaq3UfMLG3b6jlC3z0M0FRZLfW1Z5wSMFFK9g2iS5AZYDcGnhl59

