UPDATE elegance_users.clientes
SET name=CONCAT('Cliente de demostración ', id), email=CONCAT('demo-',id,'@example.invalid'),
    phone='', avatar='', notes='Fixture sintético. No corresponde a una persona real.', total_visits=0, total_spent=0
WHERE id IN ('cli-1','cli-2','cli-3','cli-4','cli-5');
UPDATE elegance_users.estilistas
SET name=CONCAT('Estilista de demostración ',id), role='Estilista de demostración', rating=0, reviews_count=0
WHERE id IN ('sty-1','sty-2','sty-3','sty-4');
UPDATE elegance_appointments.citas
SET client_name='Cliente de demostración', client_avatar='', stylist_name='Estilista de demostración', notes='Fixture sintético.'
WHERE id IN ('apt-100','apt-101','apt-102','apt-103','apt-104','apt-105');
