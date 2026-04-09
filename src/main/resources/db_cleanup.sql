-- SQL script to remove duplicate users and resources from the database.
-- Use with caution, it's recommended to take a backup first.

-- 1. Remove duplicate users (keeping the one with the smallest ID for each email)
DELETE FROM users 
WHERE id NOT IN (
    SELECT min_id FROM (
        SELECT MIN(id) AS min_id 
        FROM users 
        GROUP BY email
    ) AS temp
);

-- 2. Remove duplicate resources (assuming same title and uploader is a duplicate)
DELETE FROM resources 
WHERE id NOT IN (
    SELECT min_id FROM (
        SELECT MIN(id) AS min_id 
        FROM resources 
        GROUP BY title, uploader
    ) AS temp
);

-- 3. Reset any incorrect ID sequences (H2/MySQL)
-- For H2, sequences are managed automatically locally.
-- For MySQL, this won't be necessary unless you've manually messed with it.

SELECT 'Database cleanup complete.' AS status;
