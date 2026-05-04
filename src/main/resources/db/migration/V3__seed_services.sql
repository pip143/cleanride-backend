-- Seed initial services data
INSERT INTO services (name, description, price, duration, created_at, updated_at)
VALUES 
    ('Standard Ride', 'Regular point-to-point transportation', 25.00, 30, NOW(), NOW()),
    ('Premium Ride', 'Comfortable premium vehicle service', 40.00, 30, NOW(), NOW()),
    ('Express Ride', 'Fast express transportation service', 50.00, 20, NOW(), NOW()),
    ('Eco Ride', 'Environmentally friendly electric vehicle', 30.00, 35, NOW(), NOW()),
    ('Luxury Ride', 'High-end luxury vehicle service', 75.00, 45, NOW(), NOW())
ON CONFLICT DO NOTHING;
