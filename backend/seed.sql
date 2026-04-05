-- ParkSpot Seed Data
-- Run this in the Neon SQL Editor to populate the map with test listings and notifications.
-- Uses the first registered user as the listing owner.

DO $$
DECLARE
  owner_id UUID;
BEGIN
  SELECT id INTO owner_id FROM users ORDER BY created_at ASC LIMIT 1;

  IF owner_id IS NULL THEN
    RAISE NOTICE 'No users found. Register an account in the app first, then re-run this script.';
    RETURN;
  END IF;

  -- Insert test parking spots around Toronto
  INSERT INTO listings (user_id, address, price_per_hour, availability, description, is_active, latitude, longitude)
  VALUES
    (owner_id, '200 Bay St, Toronto, ON',      5.00, 'Mon–Fri 7am–7pm',       'Underground parking in the Financial District. Secure and covered.',         true, 43.6478, -79.3797),
    (owner_id, '1 Yonge St, Toronto, ON',       4.50, 'Daily 24/7',             'Covered parking near the waterfront. Easy TTC access.',                      true, 43.6432, -79.3771),
    (owner_id, '277 Front St W, Toronto, ON',   6.00, 'Mon–Sat 6am–10pm',      'Surface lot near Rogers Centre. Great for events.',                           true, 43.6434, -79.3892),
    (owner_id, '100 Queen St W, Toronto, ON',   3.50, 'Daily 8am–8pm',          'Outdoor parking near City Hall and Nathan Phillips Square.',                  true, 43.6526, -79.3831),
    (owner_id, '595 Bay St, Toronto, ON',        4.00, 'Mon–Fri 7am–9pm',       'Indoor parking at College Park. Steps from College subway station.',          true, 43.6599, -79.3833),
    (owner_id, '4 Bloor St E, Toronto, ON',      5.50, 'Daily 7am–midnight',    'Prime Bloor–Yonge location. Walking distance to shops and restaurants.',      true, 43.6709, -79.3857),
    (owner_id, '220 King St W, Toronto, ON',     4.75, 'Daily 24/7',             'Secure underground parking in the Entertainment District.',                  true, 43.6467, -79.3882),
    (owner_id, '25 York St, Toronto, ON',        3.00, 'Mon–Fri 6am–8pm',       'Open lot near Union Station. Perfect for commuters.',                         true, 43.6454, -79.3806)
  ON CONFLICT DO NOTHING;

  -- Insert welcome notifications for the user
  INSERT INTO notifications (user_id, title, message)
  VALUES
    (owner_id, 'Welcome to ParkSpot!',  'Thanks for joining. Start exploring parking spots near you on the map.'),
    (owner_id, 'Listing Tip',           'Add accurate addresses when creating listings so drivers can find your spot easily.')
  ON CONFLICT DO NOTHING;

  RAISE NOTICE 'Seed data inserted for user %', owner_id;
END $$;
